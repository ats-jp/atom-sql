package jp.ats.atomsql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import jp.ats.atomsql.annotation.SqlProxy;

/**
 * Spring FrameworkやDBが稼働していない環境でも、実装した{@link SqlProxy}から出力されるSQL文の確認などを行えるようにするサンドボックス環境を提供するクラスです。<br>
 * サンドボックス環境内で実行された{@link SqlProxy}の処理は、すべて{JdbcTemplate}には渡されず、DBに対する実際の操作は何も行われないので確認やテストなどを安全に行うことが可能です。
 * @author 千葉 哲嗣
 */
public class Sandbox {

	private static final ThreadLocal<List<Pair>> pairs = new ThreadLocal<>();

	private static final ThreadLocal<Result> resultHolder = new ThreadLocal<>();

	/**
	 * このサンドボックス環境用の{@link AtomSql}が提供されるので、使用者はそれにより{@SqlProxy}を生成、検査を行います。
	 * @param process 検査したい処理
	 */
	public static void execute(Consumer<AtomSql> process) {
		AtomSqlInitializer.initializeIfUninitialized();
		pairs.set(new LinkedList<Pair>());
		try {
			process.accept(new AtomSql(new Endpoints(new SandboxEndpoint())));
		} finally {
			pairs.remove();
			resultHolder.remove();
		}
	}

	/**
	 * このサンドボックス環境用の{@link AtomSql}が提供されるので、使用者はそれにより{@SqlProxy}を生成、検査を行います。<br>
	 * デフォルトではない設定を使用できます。
	 * @param config {@link Configure}
	 * @param process 検査したい処理
	 */
	public static void execute(Configure config, Consumer<AtomSql> process) {
		AtomSql.initialize(config);
		pairs.set(new LinkedList<Pair>());
		try {
			process.accept(new AtomSql(new Endpoints(new SandboxEndpoint())));
		} finally {
			pairs.remove();
			resultHolder.remove();
		}
	}

	/**
	 * サンドボックス処理内で永続するダミーの検索結果を設定します。<br>
	 * 再設定しない限り、同じ検索結果となります。
	 * @param consumer {@link Result}に値をセットする{@link Consumer}
	 */
	public static void resultSet(Consumer<Result> consumer) {
		var result = new Result();
		consumer.accept(result);
		resultHolder.set(result);
	}

	private static class SandboxEndpoint implements Endpoint {

		@Override
		public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
			var size = bpss.getBatchSize();
			for (var i = 0; i < size; i++) {
				try {
					bpss.setValues(preparedStatement(), i);
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}

			return new int[size];
		}

		@Override
		public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
			var statement = preparedStatement();

			try {
				pss.setValues(statement);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}

			var result = resultHolder.get();
			if (result == null) return Stream.of();

			int[] i = { 0 };
			return result.convert().stream().map(row -> {
				var handler = new ResultSetInvocationHandler(row);
				var resultSet = (ResultSet) Proxy.newProxyInstance(
					Sandbox.class.getClassLoader(),
					new Class<?>[] { ResultSet.class },
					handler);

				try {
					return rowMapper.mapRow(resultSet, i[0]++);
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			});
		}

		@Override
		public int update(String sql, PreparedStatementSetter pss) {
			var statement = preparedStatement();

			try {
				pss.setValues(statement);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}

			return 0;
		}

		@Override
		public void logSql(Log log, String originalSql, String sql, PreparedStatement ps) {
			var handler = pairs.get().stream().filter(p -> p.statement == ps).findFirst().get().handler;

			log.info("sql:" + Constants.NEW_LINE + originalSql);

			log.info("processed sql:" + Constants.NEW_LINE + sql);

			log.info("binding values:");

			handler.allArgs.forEach(a -> {
				var args = Arrays.stream(a.args)
					.map(v -> AtomSqlUtils.toStringForBindingValue(v))
					.toList();
				log.info(a.method.getName() + "(" + String.join(", ", args) + ")");
			});
		}

		@Override
		public void bollowConnection(Consumer<ConnectionProxy> consumer) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * ダミー検索結果セット用クラス
	 */
	public static class Result {

		private String[] columnNames = {};

		private final List<Object[]> rows = new LinkedList<>();

		/**
		 * 検索結果の全項目名をセットします。
		 * @param columnNames 検索結果の全項目名
		 */
		public void setColumnNames(String... columnNames) {
			this.columnNames = columnNames.clone();
		}

		/**
		 * 検索結果を一行追加します。
		 * @param values 検索結果一行
		 */
		public void addRow(Object... values) {
			rows.add(values.clone());
		}

		private List<Map<String, Object>> convert() {
			return rows.stream().map(r -> {
				Map<String, Object> map = new HashMap<>();
				for (var i = 0; i < columnNames.length; i++) {
					map.put(columnNames[i], r[i]);
				}

				return map;
			}).toList();
		}
	}

	private static record Pair(
		PreparedStatement statement,
		PreparedStatementInvocationHandler handler) {
	}

	private static PreparedStatement preparedStatement() {
		var handler = new PreparedStatementInvocationHandler();
		var statement = (PreparedStatement) Proxy.newProxyInstance(
			Sandbox.class.getClassLoader(),
			new Class<?>[] { PreparedStatement.class },
			handler);

		var pair = new Pair(statement, handler);

		pairs.get().add(pair);

		return statement;
	}

	private static class PreparedStatementInvocationHandler implements InvocationHandler {

		private final List<Argument> allArgs = new LinkedList<>();

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var arg = new Argument(method, args);

			allArgs.add(arg);

			return null;
		}
	}

	private static class ResultSetInvocationHandler implements InvocationHandler {

		private final Map<String, Object> map;

		private ResultSetInvocationHandler(Map<String, Object> map) {
			this.map = map;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return map.get(args[0]);
		}
	}

	private static record Argument(Method method, Object[] args) {
	}
}

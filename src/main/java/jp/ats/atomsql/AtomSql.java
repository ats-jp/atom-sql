package jp.ats.atomsql;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jp.ats.atomsql.annotation.ConfidentialSql;
import jp.ats.atomsql.annotation.Qualifier;
import jp.ats.atomsql.annotation.Sql;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.processor.annotation.Methods;

/**
 * Atom SQLの実行時の処理のほとんどを行うコアクラスです。<br>
 * {@link SqlProxy}の生成および更新処理のバッチ実行の操作が可能です。
 * @author 千葉 哲嗣
 */
public class AtomSql {

	static final Log log = LogFactory.getLog(AtomSql.class);

	private final Configure config;

	private final SqlLogger sqlLogger;

	private static final String packageName = AtomSql.class.getPackageName();

	private final ThreadLocal<BatchResources> batchResources = new ThreadLocal<>();

	private final ThreadLocal<List<Stream<?>>> streams = new ThreadLocal<>();

	private final Executors executors;

	private final SqlProxyInvocationHandler handler = new SqlProxyInvocationHandler();

	static class BatchResources {

		Map<String, Map<String, List<SqlProxyHelper>>> resources = new HashMap<>();

		void put(String name, SqlProxyHelper helper) {
			resources.computeIfAbsent(name, n -> new HashMap<>()).computeIfAbsent(helper.sql, s -> new LinkedList<>()).add(helper);
		}

		private void forEach(BatchResourceConsumer consumer) {
			resources.forEach((name, map) -> {
				map.forEach((sql, helpers) -> {
					consumer.accept(name, sql, helpers);
				});
			});
		}
	}

	@FunctionalInterface
	static interface BatchResourceConsumer {

		void accept(String name, String sql, List<SqlProxyHelper> helpers);
	}

	/**
	 * 通常のコンストラクタです。<br>
	 * {@link Executors}の持つ{@link Executor}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param executors {@link Executors}
	 */
	public AtomSql(Executors executors) {
		config = new PropertiesConfigure();
		sqlLogger = new SqlLogger(config);
		this.executors = Objects.requireNonNull(executors);
	}

	/**
	 * 通常のコンストラクタです。<br>
	 * {@link Executors}の持つ{@link Executor}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param config {@link Configure}
	 * @param executors {@link Executors}
	 */
	public AtomSql(Configure config, Executors executors) {
		this.config = Objects.requireNonNull(config);
		sqlLogger = new SqlLogger(config);
		this.executors = Objects.requireNonNull(executors);
	}

	/**
	 * コピーコンストラクタです。<br>
	 * baseと同じ接続先をもつ別インスタンスが生成されます。<br>
	 * バッチの実施単位を分けたい場合などに使用します。
	 * @param base コピー元
	 */
	public AtomSql(AtomSql base) {
		config = base.config;
		sqlLogger = base.sqlLogger;
		this.executors = base.executors;
	}

	AtomSql() {
		config = new Configure() {

			@Override
			public boolean enableLog() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Pattern logStackTracePattern() {
				throw new UnsupportedOperationException();
			}
		};

		sqlLogger = new SqlLogger(config);

		executors = new Executors(new Executor() {

			@Override
			public void batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
				throw new UnsupportedOperationException();
			}

			@Override
			public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
				throw new UnsupportedOperationException();
			}

			@Override
			public int update(String sql, PreparedStatementSetter pss) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void logSql(Log log, String originalSql, String sql, boolean confidential, PreparedStatement ps) {
				throw new UnsupportedOperationException();
			}
		});
	}

	/**
	 * {@link SqlProxy}が付与されたインターフェイスから{@link Proxy}オブジェクトを作成します。
	 * @see Proxy
	 * @see SqlProxy
	 * @param <T> 生成される{@link Proxy}の型
	 * @param proxyInterface {@link SqlProxy}が付与されたインターフェイス
	 * @return 生成された{@link Proxy}
	 * @throws IllegalArgumentException proxyInterfaceがインターフェイスではない場合
	 * @throws IllegalArgumentException proxyInterfaceに{@link SqlProxy}が付与されていない場合
	 */
	public <T> T of(Class<T> proxyInterface) {
		if (!proxyInterface.isInterface())
			//proxyInterfaceはインターフェイスではありません
			throw new IllegalArgumentException(proxyInterface + " is not interface");

		if (!proxyInterface.isAnnotationPresent(SqlProxy.class))
			//アノテーションSqlProxyが見つかりません
			throw new IllegalArgumentException("Annotation " + SqlProxy.class.getSimpleName() + " is not found");

		@SuppressWarnings("unchecked")
		T instance = (T) Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class<?>[] { proxyInterface },
			handler);

		return instance;
	}

	/**
	 * バッチ処理を実施します。<br>
	 * {@link Runnable}内で行われる更新処理はすべて、即時実行はされずに集められ、{@Runnable}の処理が終了したのち一括で実行されます。<br>
	 * 大量の更新処理を行わなければならない場合、処理の高速化を見込むことが可能です。
	 * @param runnable 更新処理を含む汎用処理
	 */
	public void tryBatch(Runnable runnable) {
		batchResources.set(new BatchResources());
		try {
			runnable.run();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	/**
	 * バッチ処理を実施します。<br>
	 * {@link Runnable}と違い、何らかの処理結果を取り出したい場合に使用します<br>
	 * @param <T> 返却値の型
	 * @see #tryBatch(Runnable)
	 * @param supplier 結果を返却が可能な更新処理を含む汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryBatch(Supplier<T> supplier) {
		batchResources.set(new BatchResources());
		try {
			return supplier.get();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	BatchResources batchResources() {
		return batchResources.get();
	}

	private void executeBatch() {
		batchResources.get().forEach((name, sql, helpers) -> {
			var startNanos = System.nanoTime();
			try {
				executors.get(name).executor().batchUpdate(sql, new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						helpers.get(i).setValues(ps);
					}

					@Override
					public int getBatchSize() {
						int size = helpers.size();

						sqlLogger.perform(log -> log.info("batch size: " + size));

						return size;
					}
				});
			} finally {
				logElapsed(sqlLogger, startNanos);
			}
		});
	}

	/**
	 * {@link Stream}を検索結果として使用する処理を実施します。<br>
	 * 処理内で発生した{@link Stream}は{@link Stream#close()}を明示的に行わなくても処理終了と同時にすべてクローズされます。
	 * @see Atom#stream()
	 * @see Atom#stream(RowMapper)
	 * @see Atom#stream(SimpleRowMapper)
	 * @param runnable {@link Stream}を使用した検索処理を含む汎用処理
	 */
	public void tryStream(Runnable runnable) {
		streams.set(new LinkedList<>());
		try {
			runnable.run();
		} finally {
			closeStreams();
			streams.remove();
		}
	}

	/**
	 * {@link Stream}を検索結果として使用する処理を実施します。<br>
	 * 処理内で発生した{@link Stream}は{@link Stream#close()}を明示的に行わなくても処理終了と同時にすべてクローズされます。
	 * {@link Runnable}と違い、何らかの処理結果を取り出したい場合に使用します<br>
	 * @param <T> 返却値の型
	 * @see #tryStream(Runnable)
	 * @param supplier {@link Stream}を使用した検索処理を含む汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryStream(Supplier<T> supplier) {
		streams.set(new LinkedList<>());
		try {
			return supplier.get();
		} finally {
			closeStreams();
			streams.remove();
		}
	}

	void registerStream(Stream<?> stream) {
		var list = streams.get();
		if (list == null) return;

		list.add(stream);
	}

	SqlProxyHelper helper(String sql) {
		return new SqlProxyHelper(
			sql,
			executors.get(),
			false,
			new String[0],
			Object.class,
			new Object[0],
			config,
			sqlLogger);
	}

	private void closeStreams() {
		streams.get().forEach(s -> {
			try {
				s.close();
			} catch (Throwable t) {
				log.warn("Error occured while Stream closing", t);
			}
		});
	}

	private class SqlProxyInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var proxyClass = proxy.getClass().getInterfaces()[0];

			//メソッドに付与されたアノテーション > クラスに付与されたアノテーション
			var nameAnnotation = method.getAnnotation(Qualifier.class);
			if (nameAnnotation == null) nameAnnotation = proxyClass.getAnnotation(Qualifier.class);

			var proxyClassName = proxyClass.getName();

			var sql = loadSql(proxyClass, method).trim();

			var methods = Class.forName(
				proxyClassName + Constants.METADATA_CLASS_SUFFIX,
				true,
				Thread.currentThread().getContextClassLoader()).getAnnotation(Methods.class);

			var find = Arrays.asList(methods.value())
				.stream()
				.filter(
					m -> m.name().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), m.parameterTypes()))
				.findFirst()
				.get();

			var parameterTypes = find.parameterTypes();

			var confidential = method.getAnnotation(ConfidentialSql.class) != null;

			SqlProxyHelper helper;
			var entry = nameAnnotation == null ? executors.get() : executors.get(nameAnnotation.value());
			if (parameterTypes.length == 1 && parameterTypes[0].equals(Consumer.class)) {
				var sqlParametersClass = find.sqlParametersClass();
				var sqlParameters = sqlParametersClass.getConstructor().newInstance();

				Consumer.class.getMethod("accept", Object.class).invoke(args[0], new Object[] { sqlParameters });

				var names = new LinkedList<String>();
				var values = new LinkedList<Object>();
				Arrays.stream(sqlParametersClass.getFields()).forEach(f -> {
					names.add(f.getName());
					try {
						values.add(f.get(sqlParameters));
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				});

				helper = new SqlProxyHelper(
					sql,
					entry,
					confidential,
					names.toArray(String[]::new),
					find.dataObjectClass(),
					values.toArray(Object[]::new),
					config,
					sqlLogger);
			} else {
				helper = new SqlProxyHelper(
					sql,
					entry,
					confidential,
					find.parameters(),
					find.dataObjectClass(),
					args,
					config,
					sqlLogger);
			}

			var atom = new Atom<Object>(AtomSql.this, helper, true);

			var returnType = method.getReturnType();

			if (returnType.equals(Atom.class)) {
				return atom;
			} else if (returnType.equals(Stream.class)) {
				return atom.stream();
			} else if (returnType.equals(List.class)) {
				return atom.list();
			} else if (returnType.equals(Optional.class)) {
				return atom.get();
			} else if (returnType.equals(int.class)) {
				return atom.update();
			} else {
				//不正な戻り値の型
				throw new IllegalStateException("Incorrect return type: " + returnType);
			}
		}
	}

	private static void logElapsed(SqlLogger sqlLogger, long startNanos) {
		sqlLogger.perform(log -> {
			var elapsed = (System.nanoTime() - startNanos) / 1000000f;
			log.info("elapsed: " + new BigDecimal(elapsed).setScale(2, RoundingMode.DOWN) + "ms");
		});
	}

	private static String loadSql(Class<?> decreredClass, Method method) throws IOException {
		var proxyClassName = decreredClass.getName();

		var sqlContainer = method.getAnnotation(Sql.class);
		if (sqlContainer != null) {
			return sqlContainer.value();
		}

		var sqlFileName = Utils.extractSimpleClassName(proxyClassName, decreredClass.getPackage().getName())
			+ "."
			+ method.getName()
			+ ".sql";

		var url = decreredClass.getResource(sqlFileName);
		if (url == null)
			//sqlFileNameが見つかりませんでした
			throw new IllegalStateException(sqlFileName + " was not found");

		return new String(Utils.readBytes(url.openStream()), Constants.CHARSET);
	}

	static class SqlProxyHelper implements PreparedStatementSetter {

		final String sql;

		final String originalSql;

		final Executors.Entry entry;

		private final boolean confidential;

		private final List<AtomSqlType> argumentTypes = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private final Class<?> dataObjectClass;

		private final Configure config;

		private final SqlLogger sqlLogger;

		private SqlProxyHelper(
			String sql,
			Executors.Entry entry,
			boolean confidential,
			String[] parameterNames,
			Class<?> dataObjectClass,
			Object[] args,
			Configure config,
			SqlLogger sqlLogger) {
			originalSql = sql;
			this.entry = entry;
			this.confidential = confidential;
			this.dataObjectClass = dataObjectClass;
			this.config = config;
			this.sqlLogger = sqlLogger;

			var argMap = new HashMap<String, Object>();
			for (int i = 0; i < parameterNames.length; i++) {
				argMap.put(parameterNames[i], args[i]);
			}

			var converted = new StringBuilder();

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				converted.append(f.gap);

				if (!argMap.containsKey(f.placeholder))
					//プレースホルダplaceholderは見つかりませんでした
					throw new IllegalStateException("Place holder [" + f.placeholder + "] was not found");

				var value = argMap.get(f.placeholder);

				var type = AtomSqlType.selectForPreparedStatement(value);

				converted.append(type.placeholderExpression(value));

				argumentTypes.add(type);
				values.add(value);
			});

			converted.append(sqlRemain);

			this.sql = converted.toString();
		}

		SqlProxyHelper(
			String sql,
			String originalSql,
			SqlProxyHelper main,
			SqlProxyHelper sub) {
			this.sql = sql;
			this.originalSql = originalSql;
			this.entry = main.entry;
			this.dataObjectClass = main.dataObjectClass;
			this.config = main.config;
			this.sqlLogger = main.sqlLogger;

			// confidentialではない場合伝染する
			this.confidential = main.confidential || sub.confidential;

			argumentTypes.addAll(main.argumentTypes);
			argumentTypes.addAll(sub.argumentTypes);

			values.addAll(main.values);
			values.addAll(sub.values);
		}

		Object createDataObject(ResultSet rs) {
			if (dataObjectClass == Object.class)
				throw new IllegalStateException();

			if (dataObjectClass.isRecord()) {
				return createRecordDataObject(dataObjectClass, rs);
			}

			Constructor<?> constructor;
			try {
				constructor = dataObjectClass.getConstructor(ResultSet.class);
			} catch (NoSuchMethodException e) {
				return createNoParametersConstructorDataObject(rs);
			}

			try {
				return constructor.newInstance(rs);
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException(e);
			}
		}

		private Object createRecordDataObject(Class<?> dataObjectClass, ResultSet rs) {
			Methods methods;
			try {
				methods = Class.forName(
					dataObjectClass.getName() + Constants.METADATA_CLASS_SUFFIX,
					true,
					Thread.currentThread().getContextClassLoader()).getAnnotation(Methods.class);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}

			var method = methods.value()[0];
			var parameterNames = method.parameters();
			var parameterTypes = method.parameterTypes();
			var parameters = new Object[parameterNames.length];

			for (var i = 0; i < parameterNames.length; i++) {
				var type = AtomSqlType.selectForResultSet(parameterTypes[i]);
				try {
					parameters[i] = type.get(rs, parameterNames[i]);
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				}
			}

			try {
				var constructor = dataObjectClass.getConstructor(parameterTypes);
				return constructor.newInstance(parameters);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException(e);
			}
		}

		private Object createNoParametersConstructorDataObject(ResultSet rs) {
			Object object;
			try {
				var constructor = dataObjectClass.getConstructor();
				object = constructor.newInstance();
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException(e);
			}

			Arrays.stream(dataObjectClass.getFields()).forEach(f -> {
				var type = AtomSqlType.selectForResultSet(f.getType());
				try {
					f.set(object, type.get(rs, f.getName()));
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			});

			return object;
		}

		void logElapsed(long startNanos) {
			AtomSql.logElapsed(sqlLogger, startNanos);
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			var i = 0;
			var size = argumentTypes.size();
			for (; i < size; i++) {
				argumentTypes.get(i).bind(i + 1, ps, values.get(i));
			}

			sqlLogger.perform(log -> {
				log.info("------ SQL START ------");

				if (entry.name() != null) {
					log.info("name: " + entry.name());
				}

				log.info("call from:");
				var elements = new Throwable().getStackTrace();
				for (var element : elements) {
					var elementString = element.toString();

					if (elementString.contains(packageName) || elementString.contains("(Unknown Source)"))
						continue;

					if (config.logStackTracePattern().matcher(elementString).find())
						log.info(" " + elementString);
				}

				log.info("sql:");

				entry.executor().logSql(log, originalSql, sql, confidential, ps);

				log.info("------  SQL END  ------");
			});
		}
	}
}

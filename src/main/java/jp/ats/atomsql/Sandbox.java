package jp.ats.atomsql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author 千葉 哲嗣
 */
public class Sandbox {

	static final ThreadLocal<Executor> executor = new ThreadLocal<>();

	private static final Executor myExecutor = new SandboxExecutor();

	private static final ThreadLocal<List<Pair>> pairs = new ThreadLocal<>();

	/**
	 * @param process
	 */
	public static void execute(Consumer<AtomSql> process) {
		pairs.set(new LinkedList<Pair>());
		executor.set(myExecutor);
		try {

			process.accept(new AtomSql());
		} finally {
			pairs.remove();
			executor.remove();
		}
	}

	private static class SandboxExecutor implements Executor {

		@Override
		public void batchUpdate(String sql, BatchPreparedStatementSetter pss) {
			var size = pss.getBatchSize();
			for (var i = 0; i < size; i++) {
				try {
					pss.setValues(preparedStatement(), i);
				} catch (SQLException e) {
					throw new IllegalStateException(e);
				}
			}

		}

		@Override
		public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
			var statement = preparedStatement();

			try {
				pss.setValues(statement);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}

			return null;
		}

		@Override
		public int update(String sql, PreparedStatementSetter pss) {
			var statement = preparedStatement();

			try {
				pss.setValues(statement);
			} catch (SQLException e) {
				throw new IllegalStateException();
			}

			return 0;
		}

		@Override
		public void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps) {
			var handler = pairs.get().stream().filter(p -> p.statement == ps).findFirst().get().handler;

			log.info(originalSql);

			log.info("processed to:");
			log.info(sql);

			if (insecure)
				return;

			log.info("binding values:");

			handler.allArgs.forEach(a -> {
				var args = Arrays.stream(a.args)
					.map(v -> v == null ? "null" : v.toString())
					.collect(Collectors.toList());
				log.info(a.method.getName() + " [" + String.join(", ", args) + "]");
			});
		}
	}

	private static class Pair {

		private PreparedStatement statement;

		private PreparedStatementInvocationHandler handler;
	}

	private static PreparedStatement preparedStatement() {
		var handler = new PreparedStatementInvocationHandler();
		var statement = (PreparedStatement) Proxy.newProxyInstance(
			Sandbox.class.getClassLoader(),
			new Class<?>[] { PreparedStatement.class },
			handler);

		var pair = new Pair();
		pair.statement = statement;
		pair.handler = handler;

		pairs.get().add(pair);

		return statement;
	}

	private static class PreparedStatementInvocationHandler implements InvocationHandler {

		private final List<Argument> allArgs = new LinkedList<>();

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var arg = new Argument();
			arg.method = method;
			arg.args = args;

			allArgs.add(arg);

			return null;
		}
	}

	private static class Argument {

		private Method method;

		private Object[] args;
	}
}

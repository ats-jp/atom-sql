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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;

import jp.ats.atomsql.annotation.InsecureSql;
import jp.ats.atomsql.annotation.Sql;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.processor.annotation.Methods;

/**
 * @author 千葉 哲嗣
 */
public class AtomSql {

	static final Logger logger = LoggerFactory.getLogger(AtomSql.class);

	private final SqlLogger sqlLogger = SqlLogger.of(Configure.instance);

	private static final String packageName = AtomSql.class.getPackageName();

	private static final ThreadLocal<Map<String, List<SqlProxyHelper>>> batchResources = new ThreadLocal<>();

	private final Executor executor;

	private final SqlProxyInvocationHandler handler = new SqlProxyInvocationHandler();

	/**
	 * 
	 * @param executor
	 */
	public AtomSql(Executor executor) {
		this.executor = Objects.requireNonNull(executor);
	}

	/**
	 * 
	 * @param <T>
	 * @param proxyInterface
	 * @return T
	 */
	public <T> T of(Class<T> proxyInterface) {
		if (!proxyInterface.isInterface())
			throw new IllegalArgumentException(proxyInterface + " is not interface");

		if (!proxyInterface.isAnnotationPresent(SqlProxy.class))
			throw new IllegalStateException("annotation " + SqlProxy.class.getSimpleName() + " not found");

		@SuppressWarnings("unchecked")
		T instance = (T) Proxy.newProxyInstance(
			AtomSql.class.getClassLoader(),
			new Class<?>[] { proxyInterface },
			handler);

		return instance;
	}

	/**
	 * 
	 * @param runnable
	 */
	public void batch(Runnable runnable) {
		batchResources.set(new LinkedHashMap<>());
		try {
			runnable.run();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	/**
	 * 
	 * @param <T>
	 * @param supplier
	 * @return T
	 */
	public <T> T batch(Supplier<T> supplier) {
		batchResources.set(new LinkedHashMap<>());
		try {
			return supplier.get();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	private void executeBatch() {
		batchResources.get().forEach((sql, helpers) -> {
			var startNanos = System.nanoTime();
			try {
				executor.batchUpdate(sql, new BatchPreparedStatementSetter() {

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
				logElapsed(startNanos);
			}
		});
	}

	private class SqlProxyInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var proxyClass = proxy.getClass().getInterfaces()[0];

			var proxyClassName = proxyClass.getName();

			var sql = loadSql(proxyClass, method);

			var methods = Class.forName(proxyClassName + Constants.METADATA_CLASS_SUFFIX).getAnnotation(Methods.class);

			var find = Arrays.asList(methods.value())
				.stream()
				.filter(
					m -> m.name().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), m.parameterTypes()))
				.findFirst()
				.get();

			var parameterTypes = find.parameterTypes();

			var insecure = method.getAnnotation(InsecureSql.class) != null;

			SqlProxyHelper helper;
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
					insecure,
					names.toArray(String[]::new),
					find.dataObjectClass(),
					values.toArray(Object[]::new));
			} else {
				helper = new SqlProxyHelper(sql, insecure, find.parameters(), find.dataObjectClass(), args);
			}

			var atom = new Atom<Object>(AtomSql.this, executor, helper, true);

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
				return atom.execute();
			} else {
				// 戻り値の型が不正です
				throw new IllegalStateException("Return type is incorrect: " + returnType);
			}
		}
	}

	private void logElapsed(long startNanos) {
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
			throw new IllegalStateException(sqlFileName + " not found");

		return new String(Utils.readBytes(url.openStream()), Constants.CHARSET);
	}

	class SqlProxyHelper implements PreparedStatementSetter {

		final String sql;

		final String originalSql;

		private final boolean insecure;

		private final List<AtomSqlType> argumentTypes = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private final Class<?> dataObjectClass;

		private SqlProxyHelper(
			String sql,
			boolean insecure,
			String[] argNames,
			Class<?> dataObjectClass,
			Object[] args) {
			originalSql = sql.trim();
			this.insecure = insecure;
			this.dataObjectClass = dataObjectClass;

			var argMap = new HashMap<String, Object>();
			for (int i = 0; i < argNames.length; i++) {
				argMap.put(argNames[i], args[i]);
			}

			var converted = new StringBuilder();

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				converted.append(f.gap);

				if (!argMap.containsKey(f.placeholder))
					throw new IllegalStateException("place holder [" + f.placeholder + "] was not found");

				var value = argMap.get(f.placeholder);

				var type = AtomSqlType.select(value);

				converted.append(type.placeholderExpression(value));

				argumentTypes.add(type);
				values.add(value);
			});

			converted.append(sqlRemain);

			this.sql = converted.toString().trim();
		}

		SqlProxyHelper(String sql, String originalSql, SqlProxyHelper main, SqlProxyHelper sub) {
			this.sql = sql;
			this.originalSql = originalSql;
			this.dataObjectClass = main.dataObjectClass;

			// セキュアではない場合すべて汚染される
			this.insecure = main.insecure || sub.insecure;

			argumentTypes.addAll(main.argumentTypes);
			argumentTypes.addAll(sub.argumentTypes);

			values.addAll(main.values);
			values.addAll(sub.values);
		}

		Object createDataObject(ResultSet rs) {
			if (dataObjectClass == Object.class)
				throw new IllegalStateException();

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

		private Object createNoParametersConstructorDataObject(ResultSet rs) {
			Constructor<?> constructor;
			try {
				constructor = dataObjectClass.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}

			Object object;
			try {
				object = constructor.newInstance();
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException(e);
			}

			Arrays.stream(dataObjectClass.getFields()).forEach(f -> {
				try {
					f.set(object, rs.getObject(f.getName()));
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			});

			return object;
		}

		void logElapsed(long startNanos) {
			AtomSql.this.logElapsed(startNanos);
		}

		ThreadLocal<Map<String, List<SqlProxyHelper>>> batchResources() {
			return batchResources;
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

				log.info("call from:");
				var elements = new Throwable().getStackTrace();
				for (var element : elements) {
					var elementString = element.toString();

					if (elementString.contains(packageName) || elementString.contains("(Unknown Source)"))
						continue;

					if (Configure.instance.logStackTracePattern.matcher(elementString).find())
						log.info(" " + elementString);
				}

				log.info("sql:");

				executor.logSql(log, originalSql, sql, insecure, ps);

				log.info("------  SQL END  ------");
			});
		}
	}
}

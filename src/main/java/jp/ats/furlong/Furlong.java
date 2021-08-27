package jp.ats.furlong;

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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import jp.ats.furlong.annotation.InsecureSql;
import jp.ats.furlong.annotation.Sql;
import jp.ats.furlong.annotation.SqlProxy;
import jp.ats.furlong.processor.annotation.Methods;

/**
 * @author 千葉 哲嗣
 */
@Component
public class Furlong {

	static final Logger logger = LoggerFactory.getLogger(Furlong.class);

	private Configure config = new Configure();

	private final SqlLogger sqlLogger = SqlLogger.of(config);

	private static final String packageName = Furlong.class.getPackageName();

	private final ThreadLocal<Map<String, List<SqlProxyHelper>>> batchResources = new ThreadLocal<>();

	private final Map<Class<?>, Object> cache = new HashMap<>();

	private final Executor executor = new FurlongExecutor();

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public Furlong(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	Furlong() {
		this.jdbcTemplate = null;
	}

	public <T> T of(Class<T> proxyInterface) {
		if (!proxyInterface.isInterface())
			throw new IllegalArgumentException(proxyInterface + " is not interface");

		synchronized (cache) {
			@SuppressWarnings("unchecked")
			T instance = (T) cache.computeIfAbsent(proxyInterface,
					k -> Proxy.newProxyInstance(Furlong.class.getClassLoader(), new Class<?>[] { proxyInterface },
							new SqlProxyInvocationHandler()));

			return instance;
		}
	}

	public void batch(Runnable runnable) {
		batchResources.set(new LinkedHashMap<>());
		try {
			runnable.run();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	public <T> T batch(Supplier<T> supplier) {
		batchResources.set(new LinkedHashMap<>());
		try {
			return supplier.get();
		} finally {
			executeBatch();
			batchResources.remove();
		}
	}

	private final void executeBatch() {
		batchResources.get().forEach((sql, helpers) -> {
			var startNanos = System.nanoTime();
			try {
				executor().batchUpdate(sql, new BatchPreparedStatementSetter() {

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

	private class FurlongExecutor implements Executor {

		@Override
		public void batchUpdate(String sql, BatchPreparedStatementSetter pss) {
			jdbcTemplate.batchUpdate(sql, pss);

		}

		@Override
		public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
			return jdbcTemplate.queryForStream(sql, pss, rowMapper);
		}

		@Override
		public int update(String sql, PreparedStatementSetter pss) {
			return jdbcTemplate.update(sql, pss);
		}

		@Override
		public void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps) {
			if (insecure) {
				log.info(originalSql);
			} else {
				log.info(ps.toString());
			}
		}
	}

	private Executor executor() {
		var executor = Sandbox.executor.get();
		return executor == null ? this.executor : executor;
	}

	private class SqlProxyInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var proxyClass = proxy.getClass().getInterfaces()[0];

			if (!proxyClass.isAnnotationPresent(SqlProxy.class))
				throw new IllegalStateException("annotation " + SqlProxy.class.getSimpleName() + " not found");

			var proxyClassName = proxyClass.getName();

			var sql = loadSql(proxyClass, method);

			var methods = Class.forName(proxyClassName + Constants.METADATA_CLASS_SUFFIX).getAnnotation(Methods.class);

			var find = Arrays.asList(methods.value()).stream().filter(
					m -> m.name().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), m.argTypes()))
					.findFirst().get();

			var argTypes = find.argTypes();

			var insecure = method.getAnnotation(InsecureSql.class) != null;

			SqlProxyHelper helper;
			if (argTypes.length == 1 && argTypes[0].equals(Consumer.class)) {
				var sqlParameterClass = find.sqlParameterClass();
				var sqlParameter = sqlParameterClass.getConstructor().newInstance();

				Consumer.class.getMethod("accept", Object.class).invoke(args[0], new Object[] { sqlParameter });

				var names = new LinkedList<String>();
				var values = new LinkedList<Object>();
				Arrays.stream(sqlParameterClass.getFields()).forEach(f -> {
					names.add(f.getName());
					try {
						values.add(f.get(sqlParameter));
					} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
					}
				});

				helper = new SqlProxyHelper(sql, insecure, names.toArray(String[]::new), find.sqlParameterClass(),
						values.toArray(Object[]::new));
			} else {
				helper = new SqlProxyHelper(sql, insecure, find.args(), find.sqlParameterClass(), args);
			}

			return new Atom<Object>(Furlong.this, executor(), helper);
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
		} else {
			var sqlFileName = Utils.extractSimpleClassName(proxyClassName, decreredClass.getPackage().getName()) + "."
					+ method.getName() + ".sql";

			var url = decreredClass.getResource(sqlFileName);
			if (url == null)
				throw new IllegalStateException(sqlFileName + " not found");

			return new String(Utils.readBytes(url.openStream()), Constants.SQL_CHARSET);
		}
	}

	class SqlProxyHelper implements PreparedStatementSetter {

		final String sql;

		final String originalSql;

		private final boolean insecure;

		private final List<ParameterType> parameterTypes = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private final Class<?> dataObjectClass;

		private SqlProxyHelper(String sql, boolean insecure, String[] argNames, Class<?> dataObjectClass,
				Object[] args) {
			originalSql = sql;
			this.insecure = insecure;
			this.dataObjectClass = dataObjectClass;

			var argMap = new HashMap<String, Object>();
			for (int i = 0; i < argNames.length; i++) {
				argMap.put(argNames[i], args[i]);
			}

			var converted = new StringBuilder();

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				converted.append(f.gap);
				converted.append("?");

				if (!argMap.containsKey(f.placeholder))
					throw new IllegalStateException("place holder [" + f.placeholder + "] was not found");

				var value = argMap.get(f.placeholder);

				parameterTypes.add(ParameterType.select(value));
				values.add(value);
			});

			converted.append(sqlRemain);

			this.sql = converted.toString();
		}

		SqlProxyHelper(String sql, String originalSql, SqlProxyHelper main, SqlProxyHelper sub) {
			this.sql = sql;
			this.originalSql = originalSql;
			this.dataObjectClass = main.dataObjectClass;

			// セキュアではない場合すべて汚染される
			this.insecure = main.insecure || sub.insecure;

			parameterTypes.addAll(main.parameterTypes);
			parameterTypes.addAll(sub.parameterTypes);

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
					throw new FurlongSqlException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			});

			return object;
		}

		void logElapsed(long startNanos) {
			Furlong.this.logElapsed(startNanos);
		}

		ThreadLocal<Map<String, List<SqlProxyHelper>>> batchResources() {
			return batchResources;
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			var i = 0;
			var size = parameterTypes.size();
			for (; i < size; i++) {
				parameterTypes.get(i).bind(i + 1, ps, values.get(i));
			}

			sqlLogger.perform(log -> {
				log.info("------ SQL START ------");

				log.info("call from:");
				var elements = new Throwable().getStackTrace();
				for (var element : elements) {
					var elementString = element.toString();

					if (elementString.contains(packageName) || elementString.contains("(Unknown Source)"))
						continue;

					if (config.logStackTracePattern.matcher(elementString).find())
						log.info(" " + elementString);
				}

				log.info("sql:");

				executor().logSql(log, originalSql, sql, insecure, ps);

				log.info("------  SQL END  ------");
			});
		}
	}
}

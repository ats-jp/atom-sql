package jp.ats.furlong;

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

import jp.ats.furlong.annotation.InsecureSQL;
import jp.ats.furlong.annotation.SQL;
import jp.ats.furlong.annotation.SQLProxy;
import jp.ats.furlong.processor.annotation.Methods;

/**
 * @author 千葉 哲嗣
 */
@Component
public class Furlong {

	static final Logger logger = LoggerFactory.getLogger(Furlong.class);

	private Configure config = new Configure();

	private final SQLLogger sqlLogger = SQLLogger.of(config);

	private static final String packageName = Furlong.class.getPackageName();

	private final ThreadLocal<Map<String, List<SQLProxyHelper>>> batchResources = new ThreadLocal<>();

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
							new SQLProxyInvocationHandler()));

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
		public Object queryForStream(String sql, PreparedStatementSetter pss, RowMapper<Object> rowMapper) {
			return jdbcTemplate.queryForStream(sql, pss, rowMapper);
		}

		@Override
		public int update(String sql, PreparedStatementSetter pss) {
			return jdbcTemplate.update(sql, pss);
		}

		@Override
		public void logSQL(Logger log, String originalSQL, String sql, Method method, PreparedStatement ps) {
			if (method.getAnnotation(InsecureSQL.class) != null) {
				log.info(originalSQL);
			} else {
				log.info(ps.toString());
			}
		}
	}

	private Executor executor() {
		var executor = Sandbox.executor.get();
		return executor == null ? this.executor : executor;
	}

	private class SQLProxyInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			var proxyClass = proxy.getClass().getInterfaces()[0];

			if (!proxyClass.isAnnotationPresent(SQLProxy.class))
				throw new IllegalStateException("annotation SQLProxy not found");

			var proxyClassName = proxyClass.getName();

			String sql;

			var sqlContainer = method.getAnnotation(SQL.class);
			if (sqlContainer != null) {
				sql = sqlContainer.value();
			} else {
				var sqlFileName = Utils.extractSimpleClassName(proxyClassName, proxyClass.getPackage().getName()) + "."
						+ method.getName() + ".sql";

				var url = proxyClass.getResource(sqlFileName);
				if (url == null)
					throw new IllegalStateException(sqlFileName + " not found");

				sql = new String(Utils.readBytes(url.openStream()), Constants.SQL_CHARSET);
			}

			var methods = Class.forName(proxyClassName + Constants.METADATA_CLASS_SUFFIX).getAnnotation(Methods.class);

			var find = Arrays.asList(methods.value()).stream().filter(
					m -> m.name().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), m.argTypes()))
					.findFirst().get();

			var argTypes = find.argTypes();

			SQLProxyHelper helper;
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

				helper = new SQLProxyHelper(method, sql, names.toArray(String[]::new), find.dataObjectClass(),
						values.toArray(Object[]::new));
			} else {
				helper = new SQLProxyHelper(method, sql, find.args(), find.dataObjectClass(), args);
			}

			var returnType = method.getReturnType();

			if (returnType.equals(Stream.class)) {
				var startNanos = System.nanoTime();
				try {
					return executor().queryForStream(helper.sql, helper, (r, n) -> helper.createDataObject(r));
				} finally {
					logElapsed(startNanos);
				}
			} else if (returnType.equals(int.class)) {
				var resources = batchResources.get();
				if (resources == null) {
					var startNanos = System.nanoTime();
					try {
						return executor().update(helper.sql, helper);
					} finally {
						logElapsed(startNanos);
					}
				}

				resources.computeIfAbsent(helper.sql, s -> new LinkedList<>()).add(helper);

				return 0;
			} else {
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

	private class SQLProxyHelper implements PreparedStatementSetter {

		private final Method method;

		private final String originalSQL;

		private final String sql;

		private final Class<?> dataObjectClass;

		private final List<ParameterType> binders = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private SQLProxyHelper(Method method, String sql, String[] argNames, Class<?> dataObjectClass, Object[] args) {
			this.method = method;
			originalSQL = sql;
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

				binders.add(ParameterType.select(value));
				values.add(value);
			});

			converted.append(sqlRemain);

			this.sql = converted.toString();
		}

		private Object createDataObject(ResultSet rs) {
			if (dataObjectClass == Object.class)
				throw new IllegalStateException();

			Constructor<?> constructor;
			try {
				constructor = dataObjectClass.getConstructor(ResultSet.class);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}

			try {
				return constructor.newInstance(rs);
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			var i = 0;
			var size = binders.size();
			for (; i < size; i++) {
				binders.get(i).bind(i + 1, ps, values.get(i));
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

				executor().logSQL(log, originalSQL, sql, method, ps);

				log.info("------  SQL END  ------");
			});
		}
	}
}

package jp.ats.furlong;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;

import jp.ats.furlong.processor.annotation.Methods;

/**
 * @author 千葉 哲嗣
 */
@Component
public class Furlong {

	public static final String METADATA_CLASS_SUFFIX = "$FurlongMetadata";

	static final Logger logger = LoggerFactory.getLogger(Furlong.class);

	private Configure config = new Configure();

	private final SQLLogger sqlLogger = SQLLogger.of(config);

	private static final Charset sqlCharset = StandardCharsets.UTF_8;

	private static final String packageName = Furlong.class.getPackageName();

	private final ThreadLocal<Map<String, List<SQLProxyHelper>>> batchResources = new ThreadLocal<>();

	private final Map<Class<?>, Object> cache = new HashMap<>();

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
				jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

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
				int packageNameLength = proxyClass.getPackage().getName().length();

				var sqlFileName = proxyClassName.substring(packageNameLength == 0 ? 0 : packageNameLength + 1) + "."
						+ method.getName() + ".sql";

				var url = proxyClass.getResource(sqlFileName);
				if (url == null)
					throw new IllegalStateException(sqlFileName + " not found");

				sql = new String(Utils.readBytes(url.openStream()), sqlCharset);
			}

			var methods = Class.forName(proxyClassName + METADATA_CLASS_SUFFIX).getAnnotation(Methods.class);

			var find = Arrays.asList(methods.value()).stream().filter(
					m -> m.name().equals(method.getName()) && Arrays.equals(method.getParameterTypes(), m.argTypes()))
					.findFirst().get();

			var helper = new SQLProxyHelper(method, sql, find.args(), find.dataObjectClass(), args);

			var returnType = method.getReturnType();

			if (returnType.equals(Stream.class)) {
				var startNanos = System.nanoTime();
				try {
					return jdbcTemplate.<Object>queryForStream(helper.sql, helper,
							(r, n) -> helper.createDataObject(r));
				} finally {
					logElapsed(startNanos);
				}
			} else if (returnType.equals(int.class)) {
				var resources = batchResources.get();
				if (resources == null) {
					var startNanos = System.nanoTime();
					try {
						return jdbcTemplate.update(helper.sql, helper);
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

	private static final Pattern placeholder = Pattern.compile(":([a-zA-Z_$][a-zA-Z\\d_$]*)");

	private class SQLProxyHelper implements PreparedStatementSetter {

		private final Method method;

		private final String originalSQL;

		private final String sql;

		private final Class<?> dataObjectClass;

		private final List<Binder> binders = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private SQLProxyHelper(Method method, String sql, String[] argNames, Class<?> dataObjectClass, Object[] args) {
			this.method = method;
			originalSQL = sql;
			this.dataObjectClass = dataObjectClass;

			var argMap = new HashMap<String, Object>();
			for (int i = 0; i < argNames.length; i++) {
				argMap.put(argNames[i], args[i]);
			}

			int position = 0;
			var converted = new StringBuilder();
			while (true) {
				var matcher = placeholder.matcher(sql);

				if (!matcher.find())
					break;

				converted.append(sql.substring(0, matcher.start()));
				converted.append("?");

				position = matcher.end();

				sql = sql.substring(position);

				var placeholder = matcher.group(1);
				var value = argMap.get(placeholder);

				if (value == null)
					throw new IllegalStateException("place holder [" + placeholder + "] was not found");

				binders.add(Binder.select(value));
				values.add(value);
			}

			converted.append(sql);

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

					if (elementString.contains(packageName))
						continue;

					if (config.logStackTracePattern.matcher(elementString).find())
						log.info(" " + elementString);
				}

				log.info("sql:");

				if (method.getAnnotation(InsecureSQL.class) != null) {
					log.info(originalSQL);
				} else {
					log.info(ps.toString());
				}

				log.info("------  SQL END  ------");
			});
		}
	}
}

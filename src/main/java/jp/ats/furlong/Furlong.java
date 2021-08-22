package jp.ats.furlong;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

	private static final Charset sqlCharset = StandardCharsets.UTF_8;

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
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					helpers.get(i).setValues(ps);
				}

				@Override
				public int getBatchSize() {
					return helpers.size();
				}
			});
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

			var helper = new SQLProxyHelper(sql, find.args(), find.dataObjectClass(), args);

			var returnType = method.getReturnType();

			if (returnType.equals(Stream.class)) {
				return jdbcTemplate.<Object>queryForStream(helper.sql, helper, (r, n) -> {
					return helper.createDataObject(r);
				});
			} else if (returnType.equals(int.class)) {
				return executeUpdate(helper);
			} else if (returnType.equals(void.class)) {
				executeUpdate(helper);
				return null;
			} else {
				// 戻り値の型が不正です
				throw new IllegalStateException("Return type is incorrect: " + returnType);
			}
		}
	}

	private int executeUpdate(SQLProxyHelper helper) {
		var resources = batchResources.get();
		if (resources == null)
			return jdbcTemplate.update(helper.sql, helper);

		resources.computeIfAbsent(helper.sql, s -> new LinkedList<>()).add(helper);

		return 0;
	}

	private static class SQLProxyHelper implements PreparedStatementSetter {

		private static final Pattern placeholder = Pattern.compile(":([a-zA-Z_$][a-zA-Z\\d_$]*)");

		private final String sql;

		private final Class<?> dataObjectClass;

		private final List<Binder> binders = new ArrayList<>();

		private final List<Object> values = new ArrayList<>();

		private SQLProxyHelper(String sql, String[] argNames, Class<?> dataObjectClass, Object[] args) {
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
		}
	}
}

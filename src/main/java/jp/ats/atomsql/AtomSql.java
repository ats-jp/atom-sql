package jp.ats.atomsql;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jp.ats.atomsql.annotation.AtomSqlSupplier;
import jp.ats.atomsql.annotation.ConfidentialSql;
import jp.ats.atomsql.annotation.NonThreadSafe;
import jp.ats.atomsql.annotation.Qualifier;
import jp.ats.atomsql.annotation.Sql;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.annotation.SqlProxySupplier;
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

	private final ThreadLocal<Map<Object, SqlProxyHelper>> nonThreadSafeHelpers = new ThreadLocal<>();

	private final Endpoints endpoints;

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
	 * {@link Endpoints}の持つ{@link Endpoint}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param endpoints {@link Endpoints}
	 */
	public AtomSql(Endpoints endpoints) {
		config = new PropertiesConfigure();
		sqlLogger = new SqlLogger(config);
		this.endpoints = Objects.requireNonNull(endpoints);
	}

	/**
	 * 通常のコンストラクタです。<br>
	 * {@link Endpoints}の持つ{@link Endpoint}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param config {@link Configure}
	 * @param endpoints {@link Endpoints}
	 */
	public AtomSql(Configure config, Endpoints endpoints) {
		this.config = Objects.requireNonNull(config);
		sqlLogger = new SqlLogger(config);
		this.endpoints = Objects.requireNonNull(endpoints);
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
		this.endpoints = base.endpoints;
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

		endpoints = new Endpoints(new Endpoint() {

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
			public void logSql(Log log, String originalSql, String sql, PreparedStatement ps) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void bollowConnection(Consumer<ConnectionProxy> consumer) {
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
			(proxy, method, args) -> {
				if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);

				if (method.getAnnotation(AtomSqlSupplier.class) != null) return AtomSql.this;

				var proxyClass = proxy.getClass().getInterfaces()[0];

				//メソッドに付与されたアノテーション > クラスに付与されたアノテーション
				var nameAnnotation = method.getAnnotation(Qualifier.class);
				if (nameAnnotation == null) nameAnnotation = proxyClass.getAnnotation(Qualifier.class);

				var proxyClassName = proxyClass.getName();

				var methods = Class.forName(
					proxyClassName + Constants.METADATA_CLASS_SUFFIX,
					true,
					Thread.currentThread().getContextClassLoader()).getAnnotation(Methods.class);

				var methodName = method.getName();

				var find = Arrays.stream(methods.value())
					.filter(
						m -> m.name().equals(methodName) && Arrays.equals(method.getParameterTypes(), m.parameterTypes()))
					.findFirst()
					.get();

				var sqlProxySupplierAnnotation = method.getAnnotation(SqlProxySupplier.class);
				if (sqlProxySupplierAnnotation != null) return of(find.sqlProxy());

				var parameterTypes = find.parameterTypes();

				var confidentialSql = method.getAnnotation(ConfidentialSql.class);
				var confidentials = confidentialSql == null ? null : confidentialSql.value();

				var sql = loadSql(proxyClass, method).trim();

				SqlProxyHelper helper;
				var entry = nameAnnotation == null ? endpoints.get() : endpoints.get(nameAnnotation.value());
				if (parameterTypes.length == 1 && parameterTypes[0].equals(Consumer.class)) {
					var sqlParametersClass = find.sqlParameters();
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
						confidentials,
						names.toArray(String[]::new),
						find.dataObject(),
						values.toArray(Object[]::new),
						config,
						sqlLogger);
				} else {
					helper = new SqlProxyHelper(
						sql,
						entry,
						confidentials,
						find.parameters(),
						find.dataObject(),
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
				} else if (returnType.equals(HalfAtom.class)) {
					return new HalfAtom<>(atom, find.sqlInterpolation());
				} else {
					//不正な戻り値の型
					throw new IllegalStateException("Incorrect return type: " + returnType);
				}
			});

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
	 * {@link #tryBatch(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します<br>
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
				endpoints.get(name).endpoint().batchUpdate(sql, new BatchPreparedStatementSetter() {

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
	 * {@link #tryStream(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します<br>
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

	private void closeStreams() {
		streams.get().forEach(s -> {
			try {
				s.close();
			} catch (Throwable t) {
				log.warn("Error occured while Stream closing", t);
			}
		});
	}

	void registerStream(Stream<?> stream) {
		var list = streams.get();
		if (list == null) return;

		list.add(stream);
	}

	/**
	 * パラメーターに{@link NonThreadSafe}が付与されている型を使用する処理を実施します。<br>
	 * スレッドセーフではない値を使用した処理はすべてこの中で行われる必要があります。
	 * @see AtomSqlType
	 * @see NonThreadSafe
	 * @see NonThreadSafeException
	 * @param runnable パラメーターに{@link NonThreadSafe}が付与されている型を使用する汎用処理
	 */
	public void tryNonThreadSafe(Runnable runnable) {
		nonThreadSafeHelpers.set(new HashMap<>());
		try {
			runnable.run();
		} finally {
			nonThreadSafeHelpers.remove();
		}
	}

	/**
	 * パラメーターに{@link NonThreadSafe}が付与されている型を使用する処理を実施します。<br>
	 * スレッドセーフではない値を使用した処理はすべてこの中で行われる必要があります。<br>
	 * {@link #tryNonThreadSafe(Runnable)}と違い、何らかの処理結果を取り出したい場合に使用します。
	 * @param <T> 返却値の型
	 * @see AtomSqlType
	 * @see NonThreadSafe
	 * @see NonThreadSafeException
	 * @param supplier パラメーターに{@link NonThreadSafe}が付与されている型を使用する汎用処理
	 * @return {@link Supplier}の返却値
	 */
	public <T> T tryNonThreadSafe(Supplier<T> supplier) {
		nonThreadSafeHelpers.set(new HashMap<>());
		try {
			return supplier.get();
		} finally {
			nonThreadSafeHelpers.remove();
		}
	}

	void registerHelperForNonThreadSafe(Object key, SqlProxyHelper helper) {
		helperMap().put(key, helper);
	}

	SqlProxyHelper getHelperForNonThreadSafe(Object key) {
		var result = helperMap().get(key);
		if (result == null) throw new NonThreadSafeException();

		return result;
	}

	private Map<Object, SqlProxyHelper> helperMap() {
		var map = nonThreadSafeHelpers.get();
		if (map == null) throw new NonThreadSafeException();

		return map;
	}

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。<br>
	 * 実装によっては、処理終了時に内部で使用する{@link Connection}がクローズされる可能性があります。<br>
	 * デフォルトであるプライマリ{@link Endpoint}が使用されます。<br>
	 * 処理内では、スレッドセーフではない値を使用することが可能です。
	 * @param consumer
	 */
	public void bollowConnection(Consumer<ConnectionProxy> consumer) {
		bollowConnection(null, consumer);
	}

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。<br>
	 * 実装によっては、処理終了時に内部で使用する{@link Connection}がクローズされる可能性があります。<br>
	 * qualifierによって使用する{@link Endpoint}を選択可能です。<br>
	 * 処理内では、スレッドセーフではない値を使用することが可能です。
	 * @param qualifier {@link Qualifier}に使用する値
	 * @param consumer
	 */
	public void bollowConnection(String qualifier, Consumer<ConnectionProxy> consumer) {
		tryNonThreadSafe(() -> endpoints.get(qualifier).endpoint().bollowConnection(consumer));
	}

	SqlProxyHelper helper(String sql) {
		return new SqlProxyHelper(
			sql,
			endpoints.get(),
			null,
			new String[0],
			Object.class,
			new Object[0],
			config,
			sqlLogger);
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

		final Endpoints.Entry entry;

		//スレッドセーフではない型の値を含んでいる場合true
		final boolean containsNonThreadSaleValues;

		final Set<String> confidentials;

		final Map<String, Object> argMap;

		private final Class<?> dataObjectClass;

		private final List<AtomSqlType> argumentTypes;

		private final List<Object> values;

		private final Configure config;

		private final SqlLogger sqlLogger;

		private Set<String> confidentials(String[] confidentials, String[] parameterNames) {
			if (confidentials == null) return Collections.emptySet();

			//ConfidentialSqlが付与されているが、valueが指定されていない場合、すべて機密扱い
			if (confidentials.length == 0) {
				return new HashSet<>(Arrays.asList(parameterNames));
			}

			return new HashSet<>(Arrays.asList(confidentials));
		}

		private SqlProxyHelper(
			String sql,
			Endpoints.Entry entry,
			String[] confidentials,
			String[] parameterNames,
			Class<?> dataObjectClass,
			Object[] args,
			Configure config,
			SqlLogger sqlLogger) {
			originalSql = sql;
			this.entry = entry;
			this.confidentials = confidentials(confidentials, parameterNames);
			this.dataObjectClass = dataObjectClass;
			this.config = config;
			this.sqlLogger = sqlLogger;

			Map<String, Object> argMap = new LinkedHashMap<>();
			for (int i = 0; i < parameterNames.length; i++) {
				argMap.put(parameterNames[i], args[i]);
			}

			this.argMap = Collections.unmodifiableMap(argMap);

			var converted = new StringBuilder();

			boolean[] nonThreadSale = { false };

			List<AtomSqlType> argumentTypes = new ArrayList<>();
			List<Object> values = new ArrayList<>();

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				converted.append(f.gap);

				if (!argMap.containsKey(f.placeholder))
					throw new PlaceholderNotFoundException(f.placeholder);

				var value = argMap.get(f.placeholder);

				var type = AtomSqlType.selectForPreparedStatement(value);

				nonThreadSale[0] = nonThreadSale[0] | type.nonThreadSafe();

				converted.append(type.placeholderExpression(value));

				argumentTypes.add(type);
				values.add(value);
			});

			this.argumentTypes = Collections.unmodifiableList(argumentTypes);
			this.values = Collections.unmodifiableList(values);

			converted.append(sqlRemain);

			this.sql = converted.toString();

			containsNonThreadSaleValues = nonThreadSale[0];
		}

		SqlProxyHelper(SqlProxyHelper base, Class<?> newDataObjectClass) {
			this.sql = base.sql;
			this.originalSql = base.originalSql;
			this.entry = base.entry;
			this.containsNonThreadSaleValues = base.containsNonThreadSaleValues;
			this.confidentials = base.confidentials;
			this.argMap = base.argMap;
			this.dataObjectClass = newDataObjectClass;
			this.argumentTypes = base.argumentTypes;
			this.values = base.values;
			this.config = base.config;
			this.sqlLogger = base.sqlLogger;
		}

		SqlProxyHelper(
			String sql,
			Set<String> confidentials,
			LinkedHashMap<String, Object> argMap,
			SqlProxyHelper main) {
			originalSql = sql;
			this.entry = main.entry;
			this.confidentials = confidentials;
			this.dataObjectClass = main.dataObjectClass;
			this.argMap = argMap;
			this.config = main.config;
			this.sqlLogger = main.sqlLogger;

			var converted = new StringBuilder();

			boolean[] nonThreadSale = { false };

			List<AtomSqlType> argumentTypes = new ArrayList<>();
			List<Object> values = new ArrayList<>();

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				converted.append(f.gap);

				if (!argMap.containsKey(f.placeholder))
					throw new PlaceholderNotFoundException(f.placeholder);

				var value = argMap.get(f.placeholder);

				var type = AtomSqlType.selectForPreparedStatement(value);

				nonThreadSale[0] = nonThreadSale[0] | type.nonThreadSafe();

				converted.append(type.placeholderExpression(value));

				argumentTypes.add(type);
				values.add(value);
			});

			this.argumentTypes = Collections.unmodifiableList(argumentTypes);
			this.values = Collections.unmodifiableList(values);

			converted.append(sqlRemain);

			this.sql = converted.toString();

			containsNonThreadSaleValues = nonThreadSale[0];
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

			confidentials = new HashSet<>(main.confidentials);
			confidentials.addAll(sub.confidentials);

			List<AtomSqlType> argumentTypes = new ArrayList<>();

			argumentTypes.addAll(main.argumentTypes);
			argumentTypes.addAll(sub.argumentTypes);

			this.argumentTypes = Collections.unmodifiableList(argumentTypes);

			List<Object> values = new ArrayList<>();

			values.addAll(main.values);
			values.addAll(sub.values);

			this.values = Collections.unmodifiableList(values);

			Map<String, Object> argMap = new LinkedHashMap<>(main.argMap);
			argMap.putAll(sub.argMap);

			this.argMap = Collections.unmodifiableMap(argMap);

			containsNonThreadSaleValues = main.containsNonThreadSaleValues | sub.containsNonThreadSaleValues;
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
			var size = argumentTypes.size();
			for (var i = 0; i < size; i++) {
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

				if (confidentials.size() > 0) {
					log.info("confidential sql:" + Constants.NEW_LINE + originalSql);
					log.info("binding values:");

					argMap.forEach((k, v) -> {
						String value;
						if (confidentials.contains(k)) {
							value = Constants.CONFIDENTIAL;
						} else {
							value = Utils.toStringForBindingValue(v);
						}

						log.info(k + ": " + value);
					});
				} else {
					entry.endpoint().logSql(log, originalSql, sql, ps);
				}

				log.info("------  SQL END  ------");
			});
		}
	}
}

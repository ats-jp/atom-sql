package jp.ats.atomsql;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
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
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import jp.ats.atomsql.InnerSql.Element;
import jp.ats.atomsql.InnerSql.Placeholder;
import jp.ats.atomsql.InnerSql.Text;
import jp.ats.atomsql.annotation.AtomSqlSupplier;
import jp.ats.atomsql.annotation.ConfidentialSql;
import jp.ats.atomsql.annotation.NoSqlLog;
import jp.ats.atomsql.annotation.NonThreadSafe;
import jp.ats.atomsql.annotation.Qualifier;
import jp.ats.atomsql.annotation.Sql;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.annotation.SqlProxySupplier;
import jp.ats.atomsql.processor.annotation.Methods;
import jp.ats.atomsql.processor.annotation.OptionalDatas;

/**
 * Atom SQLの実行時の処理のほとんどを行うコアクラスです。<br>
 * {@link SqlProxy}の生成および更新処理のバッチ実行の操作が可能です。
 * @author 千葉 哲嗣
 */
public class AtomSql {

	/**
	 * AtomSqlにパラメーターの設定を適用して初期化します。
	 * @param config 設定
	 * @throws IllegalStateException 既に初期化済みの場合
	 */
	public static void initialize(Configure config) {
		AtomSqlInitializer.initialize(config);
	}

	/**
	 * AtomSqlにデフォルトの設定を適用して初期化します。
	 * @throws IllegalStateException 既に初期化済みの場合
	 */
	public static void initialize() {
		AtomSqlInitializer.initialize();
	}

	/**
	 * AtomSqlにパラメーターの設定を適用して初期化します。<br>
	 * 既に初期化済みの場合このメソッドは何も行いません。
	 * @param config 設定
	 */
	public static void initializeIfUninitialized(Configure config) {
		AtomSqlInitializer.initializeIfUninitialized(config);
	}

	/**
	 * AtomSqlにデフォルトの設定を適用して初期化します。<br>
	 * 既に初期化済みの場合このメソッドは何も行いません。
	 */
	public static void initializeIfUninitialized() {
		AtomSqlInitializer.initializeIfUninitialized();
	}

	/**
	 * 現設定値
	 * @return {@link Configure}
	 */
	public static Configure configure() {
		return AtomSqlInitializer.configure();
	}

	static final Log log = LogFactory.getLog(AtomSql.class);

	private final AtomSqlTypeFactory typeFactory;

	private final SqlLogger sqlLogger;

	private static final String packageName = AtomSql.class.getPackageName();

	private final ThreadLocal<BatchResources> batchResources = new ThreadLocal<>();

	private final ThreadLocal<List<Stream<?>>> streams = new ThreadLocal<>();

	private final ThreadLocal<Map<Object, SqlProxyHelper>> nonThreadSafeHelpers = new ThreadLocal<>();

	private final Endpoints endpoints;

	class BatchResources {

		private static record Resource(
			SqlProxyHelper helper,
			Consumer<Integer> resultConsumer,
			Optional<StackTraceElement[]> stackTrace) {
		}

		private final Map<String, Map<String, List<Resource>>> allResources = new HashMap<>();

		private final int threshold;

		private int num = 0;

		private BatchResources() {
			var threshold = configure().batchThreshold();
			this.threshold = threshold > 0 ? threshold : Integer.MAX_VALUE;
		}

		void put(String name, SqlProxyHelper helper, Consumer<Integer> resultConsumer, Optional<StackTraceElement[]> stackTrace) {
			if (num == threshold) flushAll();

			allResources.computeIfAbsent(name, n -> new HashMap<>())
				.computeIfAbsent(
					helper.sql.string(),
					s -> new ArrayList<>())
				.add(new Resource(helper, resultConsumer, stackTrace));
			num++;
		}

		private void flushAll() {
			allResources.forEach((name, map) -> {
				map.forEach((sql, resources) -> {
					flush(name, sql, resources);
				});
			});

			num = 0;
			allResources.clear();
		}

		private void flush(String name, String sql, List<Resource> resources) {
			var startNanos = System.nanoTime();
			try {
				var results = endpoints.get(name).endpoint().batchUpdate(sql, new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						var resource = resources.get(i);
						resource.helper.setValues(ps, resource.stackTrace);
					}

					@Override
					public int getBatchSize() {
						int size = resources.size();

						sqlLogger.perform(log -> log.info("batch size: " + size));

						return size;
					}
				});

				for (var i = 0; i < results.length; i++) {
					var resultConsumer = resources.get(i).resultConsumer;
					if (resultConsumer != null) resultConsumer.accept(results[i]);
				}
			} finally {
				logElapsed(sqlLogger, startNanos);
			}
		}
	}

	/**
	 * 通常のコンストラクタです。<br>
	 * {@link Endpoints}の持つ{@link Endpoint}の実装を切り替えることで、動作検証や自動テスト用に実行することが可能です。
	 * @param endpoints {@link Endpoints}
	 */
	public AtomSql(Endpoints endpoints) {
		typeFactory = AtomSqlTypeFactory.newInstance(configure().typeFactoryClass());
		sqlLogger = SqlLogger.instance();
		this.endpoints = Objects.requireNonNull(endpoints);
	}

	/**
	 * コピーコンストラクタです。<br>
	 * baseと同じ接続先をもつ別インスタンスが生成されます。<br>
	 * バッチの実施単位を分けたい場合などに使用します。
	 * @param base コピー元
	 */
	public AtomSql(AtomSql base) {
		typeFactory = base.typeFactory;
		sqlLogger = base.sqlLogger;
		this.endpoints = base.endpoints;
	}

	AtomSql() {
		typeFactory = AtomSqlTypeFactory.newInstance(configure().typeFactoryClass());
		sqlLogger = SqlLogger.instance();

		endpoints = new Endpoints(new Endpoint() {

			@Override
			public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
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
	 * 設定で{@link Qualifier}を使用するとされている場合、対象に付与された{@link Qualifier}を返す<br>
	 * 対象自体に{@link Qualifier}が無くても、その他のアノテーション自体に{@link Qualifier}が付与されていればそれを返す
	 */
	private Optional<Qualifier> qualifier(AnnotatedElement e) {
		if (!configure().usesQualifier()) return Optional.empty();

		var qualifier = e.getAnnotation(Qualifier.class);
		if (qualifier != null) return Optional.of(qualifier);

		var annotations = e.getAnnotations();

		if (annotations.length == 0) return Optional.empty();

		return Arrays.stream(annotations)
			.map(a -> a.annotationType().getAnnotation(Qualifier.class))
			.filter(q -> q != null)
			.findFirst();
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

		var noSqlLogOnClass = proxyInterface.isAnnotationPresent(NoSqlLog.class);

		@SuppressWarnings("unchecked")
		T instance = (T) Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class<?>[] { proxyInterface },
			(proxy, method, args) -> {
				if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);

				if (method.isAnnotationPresent(AtomSqlSupplier.class)) return AtomSql.this;

				var proxyClass = proxy.getClass().getInterfaces()[0];

				//メソッドに付与されたアノテーション > クラスに付与されたアノテーション
				var nameAnnotation = qualifier(method).or(() -> qualifier(proxyClass));

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

				SqlLogger mySqlLogger;
				if (configure().enableLog()) {
					var noSqlLogOnMethod = method.isAnnotationPresent(NoSqlLog.class);
					if (noSqlLogOnClass || noSqlLogOnMethod) {
						mySqlLogger = SqlLogger.noSqlLogInstance(noSqlLogOnMethod ? method.toString() : proxyInterface.toString());
					} else {
						mySqlLogger = sqlLogger;
					}
				} else {
					mySqlLogger = SqlLogger.disabled;
				}

				SqlProxyHelper helper;
				var entry = nameAnnotation.map(a -> endpoints.get(a.value())).orElseGet(() -> endpoints.get());
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
						typeFactory,
						mySqlLogger);
				} else {
					helper = new SqlProxyHelper(
						sql,
						entry,
						confidentials,
						find.parameters(),
						find.dataObject(),
						args,
						typeFactory,
						mySqlLogger);
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
		var resources = new BatchResources();
		batchResources.set(resources);
		try {
			runnable.run();
		} finally {
			try {
				resources.flushAll();
			} finally {
				//バッチ実行中にエラーが発生した場合でも必ずクリア
				batchResources.remove();
			}
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
		var resources = new BatchResources();
		batchResources.set(resources);
		try {
			return supplier.get();
		} finally {
			try {
				resources.flushAll();
			} finally {
				//バッチ実行中にエラーが発生した場合でも必ずクリア
				batchResources.remove();
			}
		}
	}

	BatchResources batchResources() {
		return batchResources.get();
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
			typeFactory,
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

		var sqlFileName = AtomSqlUtils.extractSimpleClassName(proxyClassName, decreredClass.getPackage().getName())
			+ "."
			+ method.getName()
			+ ".sql";

		var url = decreredClass.getResource(sqlFileName);
		if (url == null)
			//sqlFileNameが見つかりませんでした
			throw new IllegalStateException(sqlFileName + " was not found");

		return new String(AtomSqlUtils.readBytes(url.openStream()), Constants.CHARSET);
	}

	static class SqlProxyHelper implements PreparedStatementSetter {

		final InnerSql sql;

		final Endpoints.Entry entry;

		private final Class<?> dataObjectClass;

		private final AtomSqlTypeFactory typeFactory;

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
			AtomSqlTypeFactory typeFactory,
			SqlLogger sqlLogger) {
			this.entry = entry;
			this.dataObjectClass = dataObjectClass;
			this.typeFactory = typeFactory;
			this.sqlLogger = sqlLogger;

			Map<String, Object> argMap = new LinkedHashMap<>();
			for (int i = 0; i < parameterNames.length; i++) {
				argMap.put(parameterNames[i], args[i]);
			}

			List<Element> elements = new LinkedList<>();

			var confidentialSet = confidentials(confidentials, parameterNames);

			var sqlRemain = PlaceholderFinder.execute(sql, f -> {
				elements.add(new Text(f.gap));

				if (!argMap.containsKey(f.placeholder))
					throw new PlaceholderNotFoundException(f.placeholder);

				var value = argMap.get(f.placeholder);

				var type = typeFactory.selectForPreparedStatement(value);

				elements.add(
					new Placeholder(
						f.placeholder,
						confidentialSet.contains(f.placeholder),
						type.placeholderExpression(value),
						f.all,
						type,
						value));
			});

			elements.add(new Text(sqlRemain));

			this.sql = new InnerSql(elements);
		}

		SqlProxyHelper(SqlProxyHelper base, Class<?> newDataObjectClass) {
			this.sql = base.sql;
			this.entry = base.entry;
			this.dataObjectClass = newDataObjectClass;
			this.typeFactory = base.typeFactory;
			this.sqlLogger = base.sqlLogger;
		}

		SqlProxyHelper(
			InnerSql sql,
			SqlProxyHelper main) {
			this.sql = sql;
			this.entry = main.entry;
			this.dataObjectClass = main.dataObjectClass;
			this.typeFactory = main.typeFactory;
			this.sqlLogger = main.sqlLogger;
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

			var optionals = new Optionals();
			for (var i = 0; i < parameterNames.length; i++) {
				var parameterType = parameterTypes[i];
				var parameterName = parameterNames[i];

				var needsOptional = false;
				if (Optional.class.equals(parameterType)) {
					parameterType = optionals.get(parameterName);
					needsOptional = true;
				}

				var type = typeFactory.selectForResultSet(parameterType);
				try {
					var value = type.get(rs, parameterName);
					parameters[i] = needsOptional ? Optional.ofNullable(value) : value;
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

			var optionals = new Optionals();
			Arrays.stream(dataObjectClass.getFields()).forEach(f -> {
				var fieldName = f.getName();
				var fieldType = f.getType();

				var needsOptional = false;
				if (Optional.class.equals(fieldType)) {
					fieldType = optionals.get(fieldName);
					needsOptional = true;
				}

				var type = typeFactory.selectForResultSet(fieldType);

				try {
					var value = type.get(rs, fieldName);
					f.set(object, needsOptional ? Optional.ofNullable(value) : value);
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			});

			return object;
		}

		private class Optionals {

			Map<String, Class<?>> map;

			private Class<?> get(String name) {
				if (map == null) {
					map = new HashMap<>();

					Arrays.stream(loadOptionalDatas().value()).forEach(d -> map.put(d.name(), d.type()));
				}

				return map.get(name);
			}
		}

		private OptionalDatas loadOptionalDatas() {
			try {
				return Class.forName(
					dataObjectClass.getName() + Constants.DATA_OBJECT_METADATA_CLASS_SUFFIX,
					true,
					Thread.currentThread().getContextClassLoader()).getAnnotation(OptionalDatas.class);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		void logElapsed(long startNanos) {
			AtomSql.logElapsed(sqlLogger, startNanos);
		}

		@Override
		public void setValues(PreparedStatement ps, Optional<StackTraceElement[]> stackTrace) throws SQLException {
			int[] i = { 1 };
			sql.placeholders(p -> {
				i[0] = p.type().bind(i[0], ps, p.value(), typeFactory);
			});

			sqlLogger.perform(log -> {
				log.info("------ SQL START ------");

				if (entry.name() != null) {
					log.info("name: " + entry.name());
				}

				log.info("call from:");
				for (var element : stackTrace.get()) {
					var elementString = element.toString();

					if (elementString.contains(packageName) || elementString.contains("(Unknown Source)") || elementString.contains("<generated>"))
						continue;

					if (configure().logStackTracePattern().matcher(elementString).find())
						log.info(" " + elementString);
				}

				var placeholders = sql.placeholders();

				if (placeholders.stream().filter(p -> p.confidential()).findFirst().isPresent()) {
					log.info("confidential sql:" + Constants.NEW_LINE + sql.originalString());
					log.info("binding values:");

					placeholders.forEach(p -> {
						String name = p.name();
						String value;
						if (p.confidential()) {
							value = Constants.CONFIDENTIAL;
						} else {
							value = AtomSqlUtils.toStringForBindingValue(p.value());
						}

						log.info(name + ": " + value);
					});
				} else {
					entry.endpoint().logSql(log, sql.originalString(), sql.string(), ps);
				}

				log.info("------  SQL END  ------");
			});
		}
	}
}

package jp.ats.atomsql;

/**
 * 内部使用クラス
 */
class AtomSqlInitializer {

	private static final ThreadLocal<Configure> configHolder = ThreadLocal.withInitial(() -> configureInternal());

	private static Configure staticConfig;

	/**
	 * 
	 * @param config
	 */
	synchronized static void initialize(Configure config) {
		if (staticConfig != null) throw new IllegalStateException("Atom SQL is already initialized");
		staticConfig = config;
	}

	/**
	 * 
	 */
	static void initialize() {
		initialize(new PropertiesConfigure());
	}

	/**
	 * @param config 
	 */
	synchronized static void initializeIfUninitialized(Configure config) {
		if (staticConfig != null) return;
		staticConfig = config;
	}

	/**
	 * 
	 */
	synchronized static void initializeIfUninitialized() {
		if (staticConfig != null) return;
		initialize(new PropertiesConfigure());
	}

	private synchronized static Configure configureInternal() {
		if (staticConfig == null) throw new IllegalStateException("Atom SQL is not initialized");
		return staticConfig;
	}

	/**
	 * 
	 * @return {@link Configure}
	 */
	static Configure configure() {
		return configHolder.get();
	}
}

package jp.ats.atomsql;

/**
 * 
 */
public class AtomSqlInitializer {

	private static final ThreadLocal<Configure> configHolder = ThreadLocal.withInitial(() -> configureInternal());

	private static Configure staticConfig;

	/**
	 * 
	 * @param config
	 */
	public synchronized static void initialize(Configure config) {
		if (staticConfig != null) throw new IllegalStateException("Atom SQL is already initialized");
		staticConfig = config;
	}

	/**
	 * 
	 */
	public static void initialize() {
		initialize(new PropertiesConfigure());
	}

	/**
	 * @param config 
	 */
	public synchronized static void initializeIfUninitialized(Configure config) {
		if (staticConfig != null) return;
		staticConfig = config;
	}

	/**
	 * 
	 */
	public synchronized static void initializeIfUninitialized() {
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
	public static Configure configure() {
		return configHolder.get();
	}
}

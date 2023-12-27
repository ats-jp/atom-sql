package jp.ats.atomsql.internal;

import jp.ats.atomsql.AtomSqlTypeFactory;

@SuppressWarnings("javadoc")
public class AtomSqlTypeFactoryThreadLocal {

	private static final ThreadLocal<AtomSqlTypeFactory> typeFactory = new ThreadLocal<>();

	public static void set(AtomSqlTypeFactory factory) {
		typeFactory.set(factory);
	}

	public static AtomSqlTypeFactory typeFactory() {
		var factory = typeFactory.get();

		if (typeFactory == null) throw new IllegalStateException(AtomSqlTypeFactory.class.getName() + " is not set");

		return factory;
	}
}

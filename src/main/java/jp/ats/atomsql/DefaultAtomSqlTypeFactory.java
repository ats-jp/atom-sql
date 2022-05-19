package jp.ats.atomsql;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jp.ats.atomsql.type.BIG_DECIMAL;
import jp.ats.atomsql.type.BINARY_STREAM;
import jp.ats.atomsql.type.BLOB;
import jp.ats.atomsql.type.BOOLEAN;
import jp.ats.atomsql.type.BYTE_ARRAY;
import jp.ats.atomsql.type.CHARACTER_STREAM;
import jp.ats.atomsql.type.CLOB;
import jp.ats.atomsql.type.CSV;
import jp.ats.atomsql.type.DATE;
import jp.ats.atomsql.type.DATETIME;
import jp.ats.atomsql.type.DOUBLE;
import jp.ats.atomsql.type.FLOAT;
import jp.ats.atomsql.type.INTEGER;
import jp.ats.atomsql.type.LONG;
import jp.ats.atomsql.type.NULL;
import jp.ats.atomsql.type.OBJECT;
import jp.ats.atomsql.type.P_BOOLEAN;
import jp.ats.atomsql.type.P_DOUBLE;
import jp.ats.atomsql.type.P_FLOAT;
import jp.ats.atomsql.type.P_INT;
import jp.ats.atomsql.type.P_LONG;
import jp.ats.atomsql.type.STRING;
import jp.ats.atomsql.type.TIME;

class DefaultAtomSqlTypeFactory implements AtomSqlTypeFactory {

	private final Map<Class<?>, AtomSqlType> typeMap = new HashMap<>();

	private final Map<String, AtomSqlType> nameMap = new HashMap<>();

	private static final AtomSqlType[] allTypes = {
		BIG_DECIMAL.instance,
		BINARY_STREAM.instance,
		BLOB.instance,
		BOOLEAN.instance,
		BYTE_ARRAY.instance,
		CHARACTER_STREAM.instance,
		CLOB.instance,
		CSV.instance,
		DATE.instance,
		DATETIME.instance,
		DOUBLE.instance,
		FLOAT.instance,
		INTEGER.instance,
		LONG.instance,
		OBJECT.instance,
		P_BOOLEAN.instance,
		P_DOUBLE.instance,
		P_FLOAT.instance,
		P_INT.instance,
		P_LONG.instance,
		STRING.instance,
		TIME.instance
	};

	private static final Class<?>[] nonPrimitiveTypes = {
		BIG_DECIMAL.instance.type(),
		BINARY_STREAM.instance.type(),
		BLOB.instance.type(),
		BOOLEAN.instance.type(),
		BYTE_ARRAY.instance.type(),
		CHARACTER_STREAM.instance.type(),
		CLOB.instance.type(),
		DOUBLE.instance.type(),
		FLOAT.instance.type(),
		INTEGER.instance.type(),
		LONG.instance.type(),
		STRING.instance.type(),
		DATE.instance.type(),
		TIME.instance.type(),
		DATETIME.instance.type()
	};

	public static AtomSqlTypeFactory instance = new DefaultAtomSqlTypeFactory();

	private DefaultAtomSqlTypeFactory() {
		Arrays.stream(allTypes).forEach(b -> {
			typeMap.put(b.type(), b);
			nameMap.put(b.getClass().getSimpleName(), b);
		});

		//型ヒント用にはデフォルトのNULLが必要
		nameMap.put(NULL.class.getSimpleName(), NULL.instance);
	}

	@Override
	public AtomSqlType selectForPreparedStatement(Object o) {
		if (o == null)
			return NULL.instance;

		var type = typeMap.get(o.getClass());
		return type == null ? OBJECT.instance : type;
	}

	@Override
	public AtomSqlType selectForResultSet(Class<?> c) {
		var type = typeMap.get(Objects.requireNonNull(c));

		if (type == null) throw new UnknownSqlTypeException(c);

		return type;
	}

	@Override
	public AtomSqlType typeOf(String name) {
		var type = nameMap.get(Objects.requireNonNull(name));
		if (type == null)
			throw new UnknownSqlTypeNameException(name);

		return type;
	}

	@Override
	public AtomSqlType typeArgumentOf(String name) {
		return typeOf(name).toTypeArgument();
	}

	@Override
	public Class<?>[] nonPrimitiveTypes() {
		return nonPrimitiveTypes.clone();
	}
}

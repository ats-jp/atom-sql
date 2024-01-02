package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

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
import jp.ats.atomsql.type.ENUM;
import jp.ats.atomsql.type.FLOAT;
import jp.ats.atomsql.type.INTEGER;
import jp.ats.atomsql.type.LONG;
import jp.ats.atomsql.type.OBJECT;
import jp.ats.atomsql.type.P_BOOLEAN;
import jp.ats.atomsql.type.P_DOUBLE;
import jp.ats.atomsql.type.P_FLOAT;
import jp.ats.atomsql.type.P_INT;
import jp.ats.atomsql.type.P_LONG;
import jp.ats.atomsql.type.STRING;
import jp.ats.atomsql.type.TIME;

/**
 * {@link AtomSqlTypeFactory}のデフォルト実装です。
 * @author 千葉 哲嗣
 */
public class DefaultAtomSqlTypeFactory implements AtomSqlTypeFactory {

	private final Map<Class<?>, AtomSqlType> typeMap = new HashMap<>();

	private final Map<String, AtomSqlType> nameMap = new HashMap<>();

	private static final AtomSqlType[] singletonTypes = {
		BIG_DECIMAL.instance,
		BINARY_STREAM.instance,
		BLOB.instance,
		BOOLEAN.instance,
		BYTE_ARRAY.instance,
		CHARACTER_STREAM.instance,
		CLOB.instance,
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

	/**
	 * singleton
	 */
	public static AtomSqlTypeFactory instance = new DefaultAtomSqlTypeFactory();

	private DefaultAtomSqlTypeFactory() {
		Arrays.stream(singletonTypes).forEach(b -> {
			typeMap.put(b.type(), b);
			nameMap.put(b.getClass().getSimpleName(), b);
		});

		CSV csv = new CSV(this);
		typeMap.put(csv.type(), csv);
		nameMap.put(CSV.class.getSimpleName(), csv);
	}

	@Override
	public AtomSqlType select(Class<?> c) {
		var type = typeMap.get(Objects.requireNonNull(c));

		if (type == null) {
			if (!c.isEnum()) throw new UnknownSqlTypeException(c);

			@SuppressWarnings("unchecked")
			var enumClass = (Class<? extends Enum<?>>) c;

			return new ENUM(enumClass);
		}

		return type;
	}

	@Override
	public AtomSqlType typeOf(String name) {
		var type = nameMap.get(Objects.requireNonNull(name));

		if (type != null) return type;

		if (Arrays.stream(name.split("\\.")).filter(DefaultAtomSqlTypeFactory::filterInvalidJavaName).findFirst().isPresent()) {
			//Javaシンボルに使用できない文字が含まれていた場合
			throw new UnknownSqlTypeNameException(name);
		}

		return new PROCESSOR_ENUM(name);
	}

	private static boolean filterInvalidJavaName(String name) {
		if (!Character.isJavaIdentifierStart(name.charAt(0))) return true;

		var length = name.length();
		for (int i = 1; i < length; i++) {
			if (!Character.isJavaIdentifierPart(name.charAt(i))) return true;
		}

		return false;
	}

	@Override
	public AtomSqlType typeArgumentOf(String name) {
		return typeOf(name).toTypeArgument();
	}

	@Override
	public boolean canUse(Class<?> c) {
		if (c.isEnum()) return true;

		return Arrays.stream(nonPrimitiveTypes).filter(t -> t.equals(c)).findFirst().isPresent();
	}

	@Override
	public boolean canUseForProcessor(TypeElement type) {
		if (type.getKind() == ElementKind.ENUM) return true;

		var typeName = type.getQualifiedName().toString();
		return Arrays.stream(nonPrimitiveTypes).filter(c -> typeName.equals(c.getCanonicalName())).findFirst().isPresent();
	}

	private static class PROCESSOR_ENUM implements AtomSqlType {

		private final String enumClass;

		private PROCESSOR_ENUM(String enumClass) {
			this.enumClass = enumClass;
		}

		@Override
		public Class<?> type() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int bind(int index, PreparedStatement statement, Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(ResultSet rs, String columnLabel) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(ResultSet rs, int columnIndex) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public AtomSqlType toTypeArgument() {
			return this;
		}

		@Override
		public String typeExpression() {
			return Enum.class.getName() + "<" + enumClass + ">";
		}
	}
}

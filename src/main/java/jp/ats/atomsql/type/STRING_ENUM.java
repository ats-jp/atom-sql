package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.EnumNotFoundException;
import jp.ats.atomsql.annotation.StringEnumValue;

/**
 * {@link Enum}
 */
public class STRING_ENUM implements AtomSqlType {

	private final Class<? extends Enum<?>> enumClass;

	private final Map<String, Enum<?>> enumMap = new HashMap<>();

	private final Map<Enum<?>, String> valueMap = new HashMap<>();

	/**
	 * コンストラクタ
	 * @param enumClass
	 */
	public STRING_ENUM(Class<? extends Enum<?>> enumClass) {
		this.enumClass = Objects.requireNonNull(enumClass);

		var enums = enumClass.getEnumConstants();

		for (var e : enums) {
			var name = e.name();

			StringEnumValue enumValue;
			try {
				enumValue = enumClass.getField(name).getAnnotation(StringEnumValue.class);
			} catch (NoSuchFieldException ex) {
				throw new IllegalStateException(ex);
			}

			var value = enumValue == null ? name : enumValue.value();

			enumMap.put(value, e);
			valueMap.put(e, value);
		}
	}

	@Override
	public Class<?> type() {
		return enumClass;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.INTEGER);

		var val = Objects.requireNonNull(valueMap.get(value));
		try {
			statement.setString(index, val);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return getInternal(rs, rs.getString(columnLabel));
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		return getInternal(rs, rs.getString(columnIndex));
	}

	private Object getInternal(ResultSet rs, String value) throws SQLException {
		if (rs.wasNull()) return null;

		var e = enumMap.get(value);

		if (e == null) throw new EnumNotFoundException(enumClass, value);

		return e;
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

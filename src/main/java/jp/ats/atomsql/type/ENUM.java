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
import jp.ats.atomsql.annotation.EnumValue;

/**
 * {@link Enum}
 */
public class ENUM implements AtomSqlType {

	private final Class<? extends Enum<?>> enumClass;

	private final Map<Integer, Enum<?>> enumMap = new HashMap<>();

	private final Map<Enum<?>, Integer> valueMap = new HashMap<>();

	/**
	 * コンストラクタ
	 * @param enumClass
	 */
	public ENUM(Class<? extends Enum<?>> enumClass) {
		this.enumClass = Objects.requireNonNull(enumClass);

		var enums = enumClass.getEnumConstants();

		var fields = enumClass.getFields();

		for (var e : enums) {
			var ordinal = e.ordinal();
			var enumValue = fields[ordinal].getAnnotation(EnumValue.class);
			var value = enumValue == null ? ordinal : enumValue.value();

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
			statement.setInt(index, val);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var val = rs.getInt(columnLabel);

		if (rs.wasNull()) return null;

		var e = enumMap.get(val);

		if (e == null) throw new EnumNotFoundException(enumClass, val);

		return e;

	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

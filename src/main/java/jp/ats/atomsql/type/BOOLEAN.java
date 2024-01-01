package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Boolean}
 */
public class BOOLEAN implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BOOLEAN();

	private BOOLEAN() {}

	@Override
	public Class<?> type() {
		return Boolean.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.BOOLEAN);

		try {
			statement.setBoolean(index, (boolean) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var value = rs.getBoolean(columnLabel);
		return rs.wasNull() ? null : value;
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		var value = rs.getBoolean(columnIndex);
		return rs.wasNull() ? null : value;
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

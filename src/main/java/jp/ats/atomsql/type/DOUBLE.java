package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Double}
 */
public class DOUBLE implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new DOUBLE();

	private DOUBLE() {}

	@Override
	public Class<?> type() {
		return Double.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.DOUBLE);

		try {
			statement.setDouble(index, (double) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var value = rs.getDouble(columnLabel);
		return rs.wasNull() ? null : value;
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		var value = rs.getDouble(columnIndex);
		return rs.wasNull() ? null : value;
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

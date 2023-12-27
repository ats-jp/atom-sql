package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Float}
 */
public class FLOAT implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new FLOAT();

	private FLOAT() {}

	@Override
	public Class<?> type() {
		return Float.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setFloat(index, (float) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var val = rs.getFloat(columnLabel);
		return rs.wasNull() ? null : val;
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

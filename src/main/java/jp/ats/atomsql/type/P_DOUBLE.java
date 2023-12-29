package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * double
 */
public class P_DOUBLE implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_DOUBLE();

	private P_DOUBLE() {}

	@Override
	public Class<?> type() {
		return double.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setDouble(index, (double) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getDouble(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return jp.ats.atomsql.type.DOUBLE.instance;
	}
}

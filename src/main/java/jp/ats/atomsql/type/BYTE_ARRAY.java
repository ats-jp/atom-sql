package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.annotation.NonThreadSafe;

/**
 * byte[]
 */
@NonThreadSafe
public class BYTE_ARRAY implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BYTE_ARRAY();

	private BYTE_ARRAY() {}

	@Override
	public Class<?> type() {
		return byte[].class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setBytes(index, (byte[]) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getBytes(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}

	@Override
	public String typeExpression() {
		return "byte[]";
	}
}

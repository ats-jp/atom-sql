package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.annotation.NonThreadSafe;

/**
 * {@link Object}
 */
@NonThreadSafe
public class OBJECT implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new OBJECT();

	private OBJECT() {}

	@Override
	public Class<?> type() {
		return Object.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setObject(index, value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getObject(columnLabel);

	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getObject(columnIndex);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

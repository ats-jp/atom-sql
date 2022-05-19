package jp.ats.atomsql.type;

import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Clob}
 */
public class CLOB implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new CLOB();

	private CLOB() {
	}

	@Override
	public Class<?> type() {
		return Clob.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setClob(index, (Clob) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getClob(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}

	@Override
	public boolean nonThreadSafe() {
		return true;
	}
}

package jp.ats.atomsql.type;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Blob}
 */
public class BLOB implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BLOB();

	private BLOB() {
	}

	@Override
	public Class<?> type() {
		return Blob.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setBlob(index, (Blob) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getBlob(columnLabel);
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

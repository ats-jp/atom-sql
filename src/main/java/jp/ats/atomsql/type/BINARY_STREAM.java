package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.BinaryStream;

/**
 * {@link BinaryStream}
 */
public class BINARY_STREAM implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BINARY_STREAM();

	private BINARY_STREAM() {
	}

	@Override
	public Class<?> type() {
		return BinaryStream.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		var stream = (BinaryStream) value;
		try {
			statement.setBinaryStream(index, stream.input, stream.length);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return new BinaryStream(rs.getBinaryStream(columnLabel), -1);
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

package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.CharacterStream;
import jp.ats.atomsql.annotation.NonThreadSafe;

/**
 * {@link CharacterStream}
 */
@NonThreadSafe
public class CHARACTER_STREAM implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new CHARACTER_STREAM();

	private CHARACTER_STREAM() {}

	@Override
	public Class<?> type() {
		return CharacterStream.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.LONGNVARCHAR);

		try {
			var stream = (CharacterStream) value;

			statement.setCharacterStream(index, stream.input, stream.length);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return new CharacterStream(rs.getCharacterStream(columnLabel), -1);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

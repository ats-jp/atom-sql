package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link Integer}
 */
public class INTEGER implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new INTEGER();

	private INTEGER() {}

	@Override
	public Class<?> type() {
		return Integer.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		if (value == null) return NullBinder.bind(index, statement, Types.INTEGER);

		try {
			statement.setInt(index, (int) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		var val = rs.getInt(columnLabel);
		return rs.wasNull() ? null : val;
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

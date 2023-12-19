package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * {@link String}
 */
public class STRING implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new STRING();

	private STRING() {}

	@Override
	public Class<?> type() {
		return String.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		try {
			statement.setString(index, (String) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getString(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

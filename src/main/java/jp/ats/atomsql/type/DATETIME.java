package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * {@link LocalDateTime}
 */
public class DATETIME implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new DATETIME();

	private DATETIME() {
	}

	@Override
	public Class<?> type() {
		return LocalDateTime.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		try {
			statement.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public LocalDateTime get(ResultSet rs, String columnLabel) throws SQLException {
		var value = rs.getTimestamp(columnLabel);
		return value == null ? null : value.toLocalDateTime();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

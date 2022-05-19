package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link LocalTime}
 */
public class TIME implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new TIME();

	private TIME() {
	}

	@Override
	public Class<?> type() {
		return LocalTime.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setTime(index, Time.valueOf((LocalTime) value));
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public LocalTime get(ResultSet rs, String columnLabel) throws SQLException {
		var value = rs.getTime(columnLabel);
		return value == null ? null : value.toLocalTime();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

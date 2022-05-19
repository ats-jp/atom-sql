package jp.ats.atomsql.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * {@link LocalDate}
 */
public class DATE implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new DATE();

	private DATE() {
	}

	@Override
	public Class<?> type() {
		return LocalDate.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setDate(index, Date.valueOf((LocalDate) value));
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public LocalDate get(ResultSet rs, String columnLabel) throws SQLException {
		var value = rs.getDate(columnLabel);
		return value == null ? null : value.toLocalDate();
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

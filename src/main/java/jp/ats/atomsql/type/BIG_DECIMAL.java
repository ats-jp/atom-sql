package jp.ats.atomsql.type;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * {@link BigDecimal}
 */
public class BIG_DECIMAL implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new BIG_DECIMAL();

	private BIG_DECIMAL() {
	}

	@Override
	public Class<?> type() {
		return BigDecimal.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		try {
			statement.setBigDecimal(index, (BigDecimal) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getBigDecimal(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return this;
	}
}

package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * null
 */
public class NULL implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new NULL();

	private NULL() {
	}

	@Override
	public Class<?> type() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		try {
			// nullの場合はsetObject(i, null)
			// DBによってはエラーとなる可能性があるため、その場合はsetNull(int, int)の使用を検討する
			statement.setObject(index, null);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

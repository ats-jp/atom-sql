package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;

/**
 * null
 */
public class NULL implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new NULL();

	private NULL() {}

	@Override
	public Class<?> type() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			//データベースによってはエラーとなる可能性がある
			//その場合は型ヒントを使用すること
			statement.setNull(index, Types.NULL);
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
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		throw new UnsupportedOperationException();
	}
}

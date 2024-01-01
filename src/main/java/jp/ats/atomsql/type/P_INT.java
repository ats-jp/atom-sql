package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.annotation.DataObject;

/**
 * int
 */
public class P_INT implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_INT();

	private P_INT() {}

	@Override
	public Class<?> type() {
		return int.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		try {
			statement.setInt(index, (int) value);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getInt(columnLabel);
	}

	@Override
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getInt(columnIndex);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return jp.ats.atomsql.type.INTEGER.instance;
	}

	/**
	 * 単一値取得用DataObject
	 */
	@DataObject
	public static class Value {

		private final int value;

		/**
		 * コンストラクタ
		 * @param rs
		 */
		public Value(ResultSet rs) {
			try {
				value = rs.getInt(1);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		int get() {
			return value;
		}
	}
}

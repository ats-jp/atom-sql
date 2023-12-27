package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlType;

/**
 * long
 */
public class P_LONG implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_LONG();

	private P_LONG() {}

	@Override
	public Class<?> type() {
		return long.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		//ラッパー型が使用される
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getLong(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return jp.ats.atomsql.type.LONG.instance;
	}
}

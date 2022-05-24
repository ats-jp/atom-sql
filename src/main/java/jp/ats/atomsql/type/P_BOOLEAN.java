package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * boolean
 */
public class P_BOOLEAN implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_BOOLEAN();

	private P_BOOLEAN() {
	}

	@Override
	public Class<?> type() {
		return boolean.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		//ラッパー型が使用される
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getBoolean(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return jp.ats.atomsql.type.BOOLEAN.instance;
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

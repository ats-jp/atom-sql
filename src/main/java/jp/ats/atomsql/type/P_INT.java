package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;

/**
 * int
 */
public class P_INT implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_INT();

	private P_INT() {
	}

	@Override
	public Class<?> type() {
		return int.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		//ラッパー型が使用される
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getInt(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return jp.ats.atomsql.type.INTEGER.instance;
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

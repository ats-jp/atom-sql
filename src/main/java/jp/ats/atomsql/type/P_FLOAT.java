package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlType;

/**
 * float
 */
public class P_FLOAT implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_FLOAT();

	private P_FLOAT() {
	}

	@Override
	public Class<?> type() {
		return float.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		//ラッパー型が使用される
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getFloat(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return FLOAT.instance;
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

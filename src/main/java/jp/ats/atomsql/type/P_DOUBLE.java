package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlType;

/**
 * double
 */
public class P_DOUBLE implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new P_DOUBLE();

	private P_DOUBLE() {
	}

	@Override
	public Class<?> type() {
		return double.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		//ラッパー型が使用される
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getDouble(columnLabel);
	}

	@Override
	public AtomSqlType toTypeArgument() {
		return DOUBLE.instance;
	}

	@Override
	public boolean nonThreadSafe() {
		return false;
	}
}

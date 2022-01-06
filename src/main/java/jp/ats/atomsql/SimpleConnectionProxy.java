package jp.ats.atomsql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * {@link ConnectionProxy}の簡易実装クラスです。
 * @author 千葉 哲嗣
 */
public class SimpleConnectionProxy implements ConnectionProxy {

	private final Connection con;

	/**
	 * 唯一のコンストラクタです。
	 * @param con
	 */
	public SimpleConnectionProxy(Connection con) {
		this.con = con;
	}

	@Override
	public Blob createBlob() {
		try {
			return con.createBlob();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public Clob createClob() {
		try {
			return con.createClob();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public void commit() {
		try {
			con.commit();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public void rollback() {
		try {
			con.rollback();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}
}

package jp.ats.atomsql;

import java.sql.SQLException;

/**
 * {@link SQLException}を検査なし例外にするためのラッパー例外クラスです。
 * @author 千葉 哲嗣
 */
public class AtomSqlException extends RuntimeException {

	private static final long serialVersionUID = -1595978181006028117L;

	/**
	 * @param cause
	 */
	public AtomSqlException(SQLException cause) {
		super(cause);
	}
}

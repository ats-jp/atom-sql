package jp.ats.furlong;

import java.sql.SQLException;

/**
 * @author 千葉 哲嗣
 */
public class FurlongSqlException extends RuntimeException {

	private static final long serialVersionUID = -1595978181006028117L;

	public FurlongSqlException(SQLException cause) {
		super(cause);
	}
}

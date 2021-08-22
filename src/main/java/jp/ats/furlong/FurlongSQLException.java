package jp.ats.furlong;

import java.sql.SQLException;

/**
 * @author 千葉 哲嗣
 */
public class FurlongSQLException extends RuntimeException {

	private static final long serialVersionUID = -1595978181006028117L;

	public FurlongSQLException(SQLException cause) {
		super(cause);
	}
}

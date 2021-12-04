package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * org.springframework.jdbc.core.BatchPreparedStatementSetter
 * @author 千葉 哲嗣
 */
@FunctionalInterface
public interface PreparedStatementSetter {

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#setValues(PreparedStatement)
	 * @param ps
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps) throws SQLException;
}

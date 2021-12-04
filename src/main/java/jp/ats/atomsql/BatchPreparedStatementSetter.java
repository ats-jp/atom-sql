package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * org.springframework.jdbc.core.BatchPreparedStatementSetter
 * @author 千葉 哲嗣
 */
public interface BatchPreparedStatementSetter {

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#setValues(PreparedStatement, int)
	 * @param ps
	 * @param i
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, int i) throws SQLException;

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#getBatchSize()
	 * @return int
	 */
	int getBatchSize();
}

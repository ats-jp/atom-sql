package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author 千葉 哲嗣
 */
public interface Executor {

	/**
	 * @param sql
	 * @param pss
	 */
	void batchUpdate(String sql, BatchPreparedStatementSetter pss);

	/**
	 * @param <T>
	 * @param sql
	 * @param pss
	 * @param rowMapper
	 * @return {@link Stream}
	 */
	<T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper);

	/**
	 * @param sql
	 * @param pss
	 * @return int
	 */
	int update(String sql, PreparedStatementSetter pss);

	/**
	 * @param log
	 * @param originalSql
	 * @param sql
	 * @param insecure
	 * @param ps
	 */
	void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps);
}

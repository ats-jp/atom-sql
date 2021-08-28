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
interface Executor {

	void batchUpdate(String sql, BatchPreparedStatementSetter pss);

	<T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper);

	int update(String sql, PreparedStatementSetter pss);

	void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps);
}

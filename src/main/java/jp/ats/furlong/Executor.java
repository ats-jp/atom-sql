package jp.ats.furlong;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author 千葉 哲嗣
 */
public interface Executor {

	void batchUpdate(String sql, BatchPreparedStatementSetter pss);

	Object queryForStream(String sql, PreparedStatementSetter pss, RowMapper<Object> rowMapper);

	int update(String sql, PreparedStatementSetter pss);

	void logSQL(Logger log, String originalSQL, String sql, Method method, PreparedStatement ps);
}

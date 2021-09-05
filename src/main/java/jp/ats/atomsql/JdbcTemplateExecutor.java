package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author 千葉 哲嗣
 */
class JdbcTemplateExecutor implements Executor {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * @param jdbcTemplate
	 */
	public JdbcTemplateExecutor(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
	}

	@Override
	public void batchUpdate(String sql, BatchPreparedStatementSetter pss) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		jdbcTemplate.batchUpdate(Constants.NEW_LINE + sql, pss);

	}

	@Override
	public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		return jdbcTemplate.queryForStream(Constants.NEW_LINE + sql, pss, rowMapper);
	}

	@Override
	public int update(String sql, PreparedStatementSetter pss) {
		// MySQLのPareparedStatement#toString()対策でSQLの先頭に改行を付与
		return jdbcTemplate.update(Constants.NEW_LINE + sql, pss);
	}

	@Override
	public void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps) {
		if (insecure) {
			log.info(originalSql);
		} else {
			log.info(ps.toString());
		}
	}
}

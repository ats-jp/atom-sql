package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * {@link JdbcTemplate}のもつ豊富な機能のうち、Atom SQLで使用する機能のみを操作可能にするためのインターフェイスです。
 * @author 千葉 哲嗣
 */
public interface Executor {

	/**
	 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
	 * @param sql
	 * @param pss
	 */
	void batchUpdate(String sql, BatchPreparedStatementSetter pss);

	/**
	 * @see JdbcTemplate#queryForStream(String, PreparedStatementSetter, RowMapper)
	 * @param <T>
	 * @param sql
	 * @param pss
	 * @param rowMapper
	 * @return {@link Stream}
	 */
	<T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper);

	/**
	 * @see JdbcTemplate#update(String, PreparedStatementSetter)
	 * @param sql
	 * @param pss
	 * @return int
	 */
	int update(String sql, PreparedStatementSetter pss);

	/**
	 * SQLログ出力を行う設定にしている場合、実装に合わせたSQL文をログ出力します。
	 * @see Configure#enableLog
	 * @param log {@link Logger}
	 * @param originalSql プレースホルダ変換前のSQL
	 * @param sql プレースホルダ変換後のSQL
	 * @param insecure @{InsecureSql}が付与されたSQL文の場合、true
	 * @param ps プレースホルダ返還後SQLセット済みの{@link PreparedStatement}
	 */
	void logSql(Logger log, String originalSql, String sql, boolean insecure, PreparedStatement ps);
}
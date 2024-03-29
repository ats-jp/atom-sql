package jp.ats.atomsql;

import java.lang.System.Logger;
import java.sql.PreparedStatement;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * JdbcTemplateのもつ豊富な機能のうち、Atom SQLで使用する機能のみを操作可能にするためのインターフェイスです。
 * @author 千葉 哲嗣
 */
public interface Endpoint {

	/**
	 * JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)を参考にしたメソッドです。
	 * @param sql
	 * @param bpss
	 * @return affected rows
	 */
	int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss);

	/**
	 * JdbcTemplate#queryForStream(String, PreparedStatementSetter, RowMapper)を参考にしたメソッドです。
	 * @param <T>
	 * @param sql
	 * @param pss
	 * @param rowMapper
	 * @return {@link Stream}
	 */
	<T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper);

	/**
	 * JdbcTemplate#update(String, PreparedStatementSetter)を参考にしたメソッドです。
	 * @param sql
	 * @param pss
	 * @return int
	 */
	int update(String sql, PreparedStatementSetter pss);

	/**
	 * SQLログ出力を行う設定にしている場合、実装に合わせたSQL文をログ出力します。
	 * @see Configure#enableLog
	 * @param logger {@link Logger}
	 * @param originalSql プレースホルダ変換前のSQL
	 * @param sql プレースホルダ変換後のSQL
	 * @param ps プレースホルダ返還後SQLセット済みの{@link PreparedStatement}
	 */
	void logSql(Logger logger, String originalSql, String sql, PreparedStatement ps);

	/**
	 * {@link ConnectionProxy}を使用して行う処理を実施します。
	 * @param consumer
	 */
	void bollowConnection(Consumer<ConnectionProxy> consumer);
}

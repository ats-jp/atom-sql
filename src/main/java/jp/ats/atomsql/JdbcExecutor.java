package jp.ats.atomsql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;

/**
 * JDBCを使用した{@link Executor}の簡易実装クラスです。
 * @author 千葉 哲嗣
 */
public class JdbcExecutor implements Executor {

	private final Supplier<Connection> supplier;

	/**
	 * 単一のコンストラクタです。
	 * @param supplier {@link Connection}の供給元
	 */
	public JdbcExecutor(Supplier<Connection> supplier) {
		this.supplier = supplier;
	}

	@Override
	public void batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
		try (var ps = supplier.get().prepareStatement(Constants.NEW_LINE + sql)) {
			var size = bpss.getBatchSize();
			for (var i = 0; i < size; i++) {
				bpss.setValues(ps, i);
				ps.addBatch();
			}

			ps.executeBatch();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
		try {
			var ps = supplier.get().prepareStatement(Constants.NEW_LINE + sql);
			ps.closeOnCompletion();

			pss.setValues(ps);

			var rs = ps.executeQuery();

			var stream = StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(
					new ResultSetIterator<T>(rs, rowMapper),

					Spliterator.NONNULL | Spliterator.IMMUTABLE),
				false);

			stream.onClose(() -> {
				try {
					rs.close();
				} catch (SQLException e) {
					throw new AtomSqlException(e);
				}
			});

			return stream;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public int update(String sql, PreparedStatementSetter pss) {
		try (var ps = supplier.get().prepareStatement(Constants.NEW_LINE + sql)) {
			pss.setValues(ps);

			return ps.executeUpdate();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public void logSql(Log log, String originalSql, String sql, boolean confidential, PreparedStatement ps) {
		if (confidential) {
			log.info(CONFIDENTIAL + " " + originalSql);
		} else {
			log.info(ps.toString());
		}
	}

	private static class ResultSetIterator<T> implements Iterator<T> {

		private final ResultSet rs;

		private final RowMapper<T> rowMapper;

		private boolean hasNext;

		private int rowNum;

		private ResultSetIterator(ResultSet rs, RowMapper<T> rowMapper) throws SQLException {
			this.rs = rs;
			this.rowMapper = rowMapper;
			hasNext = rs.next();
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			if (!hasNext) throw new IllegalStateException();

			try {
				var row = rowMapper.mapRow(rs, ++rowNum);

				hasNext = rs.next();

				return row;
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}
	}
}

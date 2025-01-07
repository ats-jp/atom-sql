package jp.ats.atomsql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * JDBCを使用した{@link Endpoint}の簡易実装クラスです。
 * @author 千葉 哲嗣
 */
public class JdbcEndpoint implements Endpoint {

	private final Supplier<Connection> supplier;

	private final ThreadLocal<Connection> connection = new ThreadLocal<>();

	/**
	 * 単一のコンストラクタです。
	 * @param supplier {@link Connection}の供給元
	 */
	public JdbcEndpoint(Supplier<Connection> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
	}

	@Override
	public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
		try (var conn = connection()) {
			try (var ps = conn.prepareStatement(Constants.NEW_LINE + sql)) {
				var size = bpss.getBatchSize();
				for (var i = 0; i < size; i++) {
					bpss.setValues(ps, i);
					ps.addBatch();
				}

				return ps.executeBatch();
			}
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
		try {
			var conn = connection();

			var ps = conn.prepareStatement(Constants.NEW_LINE + sql);
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
				} finally {
					try {
						conn.close();
					} catch (SQLException e) {
						throw new AtomSqlException(e);
					}
				}
			});

			return stream;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public int update(String sql, PreparedStatementSetter pss) {
		try (var conn = connection()) {
			try (var ps = conn.prepareStatement(Constants.NEW_LINE + sql)) {
				pss.setValues(ps);

				return ps.executeUpdate();
			}
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	@Override
	public void logSql(Logger logger, String originalSql, String sql, PreparedStatement ps) {
		logger.log(Level.INFO, "sql:" + Constants.NEW_LINE + ps.toString());
	}

	@Override
	public void bollowConnection(Consumer<ConnectionProxy> consumer) {
		try (var conn = supplier.get()) {
			connection.set(conn);

			consumer.accept(new SimpleConnectionProxy(conn));
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		} finally {
			connection.remove();
		}
	}

	/**
	 * bollowConnection中は同一のConnectionの使用を強制する
	 */
	private Connection connection() {
		var con = connection.get();
		return con == null ? supplier.get() : con;
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

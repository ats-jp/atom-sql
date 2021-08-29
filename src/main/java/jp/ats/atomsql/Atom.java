package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.RowMapper;

import jp.ats.atomsql.AtomSql.SqlProxyHelper;

/**
 * @author 千葉 哲嗣
 * @param <T> 
 */
public class Atom<T> {

	private final AtomSql atomsql;

	private final Executor executor;

	private final SqlProxyHelper helper;

	private final boolean andType;

	Atom(AtomSql atomsql, Executor executor, SqlProxyHelper helper, boolean andType) {
		this.atomsql = atomsql;
		this.executor = executor;
		this.helper = helper;
		this.andType = andType;
	}

	/**
	 * @return results
	 */
	public Stream<T> stream() {
		var startNanos = System.nanoTime();
		try {
			return executor.queryForStream(helper.sql, helper, (r, n) -> {
				@SuppressWarnings("unchecked")
				var object = (T) helper.createDataObject(r);
				return object;
			});
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * @return results
	 */
	public List<T> list() {
		return stream().collect(Collectors.toList());
	}

	/**
	 * @return result
	 */
	public Optional<T> get() {
		return get(list());
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return results
	 */
	public <R> Stream<R> stream(RowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var startNanos = System.nanoTime();
		try {
			return executor.queryForStream(helper.sql, helper, mapper);
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return results
	 */
	public <R> List<R> list(RowMapper<R> mapper) {
		return stream(mapper).collect(Collectors.toList());
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return result
	 */
	public <R> Optional<R> get(RowMapper<R> mapper) {
		return get(list(mapper));
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return results
	 */
	public <R> Stream<R> stream(SimpleRowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var startNanos = System.nanoTime();
		try {
			return executor.queryForStream(helper.sql, helper, (r, n) -> mapper.mapRow(r));
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return results
	 */
	public <R> List<R> list(SimpleRowMapper<R> mapper) {
		return stream(mapper).collect(Collectors.toList());
	}

	/**
	 * @param mapper 
	 * @param <R> 
	 * @return result
	 */
	public <R> Optional<R> get(SimpleRowMapper<R> mapper) {
		return get(list(mapper));
	}

	/**
	 * @return SQL
	 */
	public String sql() {
		return helper.originalSql;
	}

	/**
	 * @return isEmpty
	 */
	public boolean isEmpty() {
		return helper.originalSql.isEmpty();
	}

	private <E> Optional<E> get(List<E> list) {
		if (list.size() > 1)
			throw new IllegalStateException("result row is over 1");

		return list.stream().findFirst();
	}

	/**
	 * @return updated row count
	 */
	public int execute() {
		var resources = helper.batchResources().get();
		if (resources == null) {// バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				return executor.update(helper.sql, helper);
			} finally {
				helper.logElapsed(startNanos);
			}
		}

		resources.computeIfAbsent(helper.sql, s -> new LinkedList<>()).add(helper);

		return 0;
	}

	/**
	 * @param another
	 * @return {@link Atom}
	 */
	public Atom<T> concat(Atom<?> another) {
		Objects.requireNonNull(another);

		var sql = concat(" ", helper.sql, another.helper.sql);
		var originalSql = concat(" ", helper.originalSql, another.helper.originalSql);
		return new Atom<T>(
			atomsql,
			executor,
			atomsql.new SqlProxyHelper(sql, originalSql, helper, another.helper),
			true);
	}

	/**
	 * @param another
	 * @return {@link Atom}
	 */
	public Atom<T> and(Atom<?> another) {
		return andOr(" AND ", another, true);
	}

	/**
	 * @param another
	 * @return {@link Atom}
	 */
	public Atom<T> or(Atom<?> another) {
		return andOr(" OR ", another, false);
	}

	private Atom<T> andOr(String delimiter, Atom<?> another, boolean andTypeCurrent) {
		Objects.requireNonNull(another);

		var sqls = guardSql(andType, andTypeCurrent, helper);
		var anotherSqls = guardSql(another.andType, andTypeCurrent, another.helper);

		var sql = concat(delimiter, sqls.sql, anotherSqls.sql);
		var originalSql = concat(delimiter, sqls.originalSql, anotherSqls.originalSql);

		return new Atom<T>(
			atomsql,
			executor,
			atomsql.new SqlProxyHelper(sql, originalSql, helper, another.helper),
			andTypeCurrent);
	}

	private static String concat(String delimiter, String sql1, String sql2) {
		if (sql1.isBlank()) return sql2;
		if (sql2.isBlank()) return sql1;

		return sql1 + delimiter + sql2;
	}

	private static Sqls guardSql(boolean andType, boolean andTypeCurrent, SqlProxyHelper helper) {
		var sqls = new Sqls();
		if (!andType && andTypeCurrent) {//現在ORでAND追加された場合
			sqls.sql = helper.sql.isBlank() ? "" : ("(" + helper.sql + ")");
			sqls.originalSql = helper.originalSql.isBlank() ? "" : ("(" + helper.originalSql + ")");
		} else {
			sqls.sql = helper.sql;
			sqls.originalSql = helper.originalSql;
		}

		return sqls;
	}

	private static class Sqls {

		private String sql;

		private String originalSql;
	}
}

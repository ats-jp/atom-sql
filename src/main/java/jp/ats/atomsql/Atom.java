package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.RowMapper;

import jp.ats.atomsql.AtomSql.SqlProxyHelper;

/**
 * @author 千葉 哲嗣
 */
public class Atom<T> {

	private final AtomSql atomsql;

	private final Executor executor;

	private final SqlProxyHelper helper;

	private final boolean andType;

	Atom(AtomSql atomsql, Executor executor, SqlProxyHelper helper) {
		this.atomsql = atomsql;
		this.executor = executor;
		this.helper = helper;
		this.andType = true;
	}

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

	public List<T> list() {
		return stream().collect(Collectors.toList());
	}

	public Optional<T> get() {
		return get(list());
	}

	public <R> Stream<R> stream(RowMapper<R> mapper) {
		var startNanos = System.nanoTime();
		try {
			return executor.queryForStream(helper.sql, helper, mapper);
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	public <R> List<R> list(RowMapper<R> mapper) {
		return stream(mapper).collect(Collectors.toList());
	}

	public <R> Optional<R> get(RowMapper<R> mapper) {
		return get(list(mapper));
	}

	public <R> Stream<R> stream(SimpleRowMapper<R> mapper) {
		var startNanos = System.nanoTime();
		try {
			return executor.queryForStream(helper.sql, helper, (r, n) -> mapper.mapRow(r));
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	public <R> List<R> list(SimpleRowMapper<R> mapper) {
		return stream(mapper).collect(Collectors.toList());
	}

	public <R> Optional<R> get(SimpleRowMapper<R> mapper) {
		return get(list(mapper));
	}

	private <E> Optional<E> get(List<E> list) {
		if (list.size() > 1)
			throw new IllegalStateException("result row is over 1");

		return list.stream().findFirst();
	}

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

	public Atom<T> concat(Atom<T> another) {
		var sql = helper.sql + " " + another.helper.sql;
		var originalSql = helper.originalSql + " " + another.helper.originalSql;
		return new Atom<T>(atomsql, executor, atomsql.new SqlProxyHelper(sql, originalSql, helper, another.helper));
	}

	public Atom<T> and(Atom<T> another) {
		return andor(" AND ", another);
	}

	public Atom<T> or(Atom<T> another) {
		return andor(" OR ", another);
	}

	private Atom<T> andor(String delimiter, Atom<T> another) {
		var sqls = guardSql(andType, helper);
		var anotherSqls = guardSql(another.andType, another.helper);

		var sql = sqls.sql + delimiter + anotherSqls.sql;
		var originalSql = sqls.originalSql + delimiter + anotherSqls.originalSql;

		return new Atom<T>(atomsql, executor, atomsql.new SqlProxyHelper(sql, originalSql, helper, another.helper));
	}

	private static Sqls guardSql(boolean andType, SqlProxyHelper helper) {
		var sqls = new Sqls();
		if (andType) {
			sqls.sql = helper.sql;
			sqls.originalSql = helper.originalSql;
		} else {
			sqls.sql = "(" + helper.sql + ")";
			sqls.originalSql = "(" + helper.originalSql + ")";
		}

		return sqls;
	}

	private static class Sqls {

		private String sql;

		private String originalSql;
	}
}

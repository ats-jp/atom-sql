package jp.ats.atomsql;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.RowMapper;

import jp.ats.atomsql.AtomSql.SqlProxyHelper;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlProxy;

/**
 * {@link SqlProxy}が生成する中間形態オブジェクトを表すクラスです。<br>
 * 内部にSQL文を保持しており、その実行、またSQL文が断片だった場合は断片同士の結合等を行うことが可能です。
 * @author 千葉 哲嗣
 * @param <T> {@link DataObject}が付与された型
 */
public class Atom<T> {

	private final AtomSql atomsql;

	private final SqlProxyHelper helper;

	private final boolean andType;

	Atom(AtomSql atomsql, SqlProxyHelper helper, boolean andType) {
		this.atomsql = atomsql;
		this.helper = helper;
		this.andType = andType;
	}

	/**
	 * 検索結果を{@link Stream}として返します。<br>
	 * 内部的に{@link ResultSet}を使用して逐次行取得しており、{@link Stream}の終端に到達するまで{@link ResultSet}が閉じられないので注意が必要です。<br>
	 * 検索結果全件に対して操作を行いたい（{@link Stream#map}等）、結果オブジェクトすべてが必要でない場合は{@link List}で結果取得するよりも若干効率的です。
	 * @return {@DataObject}付与結果オブジェクトの{@link Stream}
	 */
	public Stream<T> stream() {
		var startNanos = System.nanoTime();
		try {
			return helper.entry.executor.queryForStream(helper.sql, helper, (r, n) -> {
				@SuppressWarnings("unchecked")
				var object = (T) helper.createDataObject(r);
				return object;
			});
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * 検索結果を{@link List}として返します。<br>
	 * 内部的に{@link ResultSet}から全件結果取得してから{@link List}として返却しています。<br>
	 * @return {@DataObject}付与結果オブジェクトの{@link List}
	 */
	public List<T> list() {
		return listAndClose(stream());
	}

	/**
	 * 検索結果が1件であることが判明している場合（PKを条件に検索した場合等）、このメソッドを使用することでその1件のみ取得することが可能です。<br>
	 * 検索結果が0件の場合、空の{@link Optional}が返されます。<br>
	 * 検索結果が1件以上ある場合、不正な状態であると判断し例外がスローされます。
	 * @return {@link DataObject}付与型の結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public Optional<T> get() {
		return get(list());
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link Stream}として返します。<br>
	 * @see #stream
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(RowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var startNanos = System.nanoTime();
		try {
			return helper.entry.executor.queryForStream(helper.sql, helper, mapper);
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(RowMapper<R> mapper) {
		return listAndClose(stream(mapper));
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクト一件を{@link Optional}にラップして返します。<br>
	 * @see #get
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public <R> Optional<R> get(RowMapper<R> mapper) {
		return get(list(mapper));
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link Stream}として返します。<br>
	 * @see #stream
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(SimpleRowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var startNanos = System.nanoTime();
		try {
			return helper.entry.executor.queryForStream(helper.sql, helper, (r, n) -> mapper.mapRow(r));
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(SimpleRowMapper<R> mapper) {
		return listAndClose(stream(mapper));
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクト一件を{@link Optional}にラップして返します。<br>
	 * @see #get
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public <R> Optional<R> get(SimpleRowMapper<R> mapper) {
		return get(list(mapper));
	}

	/**
	 * このインスタンスが持つ?プレースホルダ変換前のSQL文もしくはその一部を返します。
	 * @return SQL文もしくはその一部
	 */
	public String sql() {
		return helper.originalSql;
	}

	/**
	 * @see Atom#sql
	 * @return SQL文もしくはその一部
	 */
	@Override
	public String toString() {
		return helper.originalSql;
	}

	/**
	 * 内部に持つSQLが空文字列かどうかを返します。
	 * @return 内部に持つSQLが空文字列である場合、true
	 */
	public boolean isEmpty() {
		return helper.originalSql.isEmpty();
	}

	private <E> Optional<E> get(List<E> list) {
		if (list.size() > 1)
			//結果は1行以下でなければなりません
			throw new IllegalStateException("The result must be less than or equal to one row");

		return list.stream().findFirst();
	}

	/**
	 * 更新処理（INSERT, UPDATE）、DML文を実行します。<br>
	 * バッチ実行の場合、結果は常に0となります。
	 * @return 更新処理の場合、その結果件数
	 */
	public int execute() {
		var resources = helper.batchResources();
		if (resources == null) {// バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				return helper.entry.executor.update(helper.sql, helper);
			} finally {
				helper.logElapsed(startNanos);
			}
		}

		resources.put(helper.entry.name, helper);

		return 0;
	}

	/**
	 * 内部に持つSQL文の一部同士を" "をはさんで文字列結合します。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。
	 * @param another 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> concat(Atom<?> another) {
		Objects.requireNonNull(another);

		var sql = concat(" ", helper.sql, another.helper.sql);
		var originalSql = concat(" ", helper.originalSql, another.helper.originalSql);
		return new Atom<T>(
			atomsql,
			atomsql.new SqlProxyHelper(sql, originalSql, helper, another.helper),
			true);
	}

	/**
	 * 内部に持つSQL文の一部同士を" AND "をはさんで文字列結合します。<br>
	 * このインスタンスかもう一方のもつSQLが空の場合、結合は行われず、SQLが空ではない側のインスタンスが返されます。<br>
	 * このインスタンスかもう一方のもつSQLが既に{@link #or}を使用して結合されたものであった場合、OR側のSQLは外側に()が付与され保護されます。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。
	 * @param another 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> and(Atom<?> another) {
		return andOr(" AND ", another, true);
	}

	/**
	 * 内部に持つSQL文の一部同士を" OR "をはさんで文字列結合します。<br>
	 * このインスタンスかもう一方のもつSQLが空の場合、結合は行われず、SQLが空ではない側のインスタンスが返されます。<br>
	 * {@link #or}を使用して結合されたインスタンスを{@link #and}を使用して結合した場合、このインスタンスのSQLは外側に()が付与され保護されます。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。
	 * @param another 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> or(Atom<?> another) {
		//どちらか一方でも空の場合OR結合が発生しないのでAND状態のままとする
		var andType = isEmpty() || another.isEmpty();
		return andOr(" OR ", another, andType);
	}

	private Atom<T> andOr(String delimiter, Atom<?> another, boolean andTypeCurrent) {
		Objects.requireNonNull(another);

		var sqls = guardSql(andType, andTypeCurrent, helper);
		var anotherSqls = guardSql(another.andType, andTypeCurrent, another.helper);

		var sql = concat(delimiter, sqls.sql, anotherSqls.sql);
		var originalSql = concat(delimiter, sqls.originalSql, anotherSqls.originalSql);

		return new Atom<T>(
			atomsql,
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

	private static <T> List<T> listAndClose(Stream<T> stream) {
		try (stream) {
			return stream.collect(Collectors.toList());
		}
	}
}

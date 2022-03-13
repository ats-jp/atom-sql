package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

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

	/**
	 * 空文字列
	 */
	public static final Atom<?> EMPTY = newInstance("");

	/**
	 * 空白
	 */
	public static final Atom<?> BLANK = newInstance(" ");

	/**
	 * ,
	 */
	public static final Atom<?> COMMA = newInstance(", ");

	/**
	 * ,<br>
	 * COMMAの短縮形
	 */
	public static final Atom<?> C = COMMA;

	/**
	 * 改行
	 */
	public static final Atom<?> CRLF = newInstance("\r\n");

	/**
	 * INNER JOIN
	 */
	public static final Atom<?> INNER_JOIN = newInstance("INNER JOIN");

	/**
	 * LEFT OUTER JOIN
	 */
	public static final Atom<?> LEFT_OUTER_JOIN = newInstance("LEFT OUTER JOIN");

	/**
	 * RIGHT OUTER JOIN
	 */
	public static final Atom<?> RIGHT_OUTER_JOIN = newInstance("RIGHT OUTER JOIN");

	/**
	 * WHERE
	 */
	public static final Atom<?> WHERE = newInstance("WHERE");

	/**
	 * HAVING
	 */
	public static final Atom<?> HAVING = newInstance("HAVING");

	/**
	 * ORDER BY
	 */
	public static final Atom<?> ORDER_BY = newInstance("ORDER BY");

	/**
	 * GROUP BY
	 */
	public static final Atom<?> GROUP_BY = newInstance("GROUP BY");

	/**
	 * (
	 */
	public static final Atom<?> LEFT_PAREN = newInstance("(");

	/**
	 * (<br>
	 * LEFT_PARENの短縮形
	 */
	public static final Atom<?> L = LEFT_PAREN;

	/**
	 * )
	 */
	public static final Atom<?> RIGHT_PAREN = newInstance(")");

	/**
	 * )<br>
	 * RIGHT_PARENの短縮形
	 */
	public static final Atom<?> R = RIGHT_PAREN;

	private final AtomSql atomSql;

	private final Supplier<SqlProxyHelper> helperSupplier;

	private final Object nonThreadSafeHelperKey = new Object();

	private final boolean andType;

	Atom(AtomSql atomsql, SqlProxyHelper helper, boolean andType) {
		this.atomSql = atomsql;

		if (helper.containsNonThreadSaleValues) {
			atomSql.registerHelperForNonThreadSafe(nonThreadSafeHelperKey, helper);
			this.helperSupplier = () -> atomSql.getHelperForNonThreadSafe(nonThreadSafeHelperKey);
		} else {
			this.helperSupplier = () -> helper;
		}

		this.andType = andType;
	}

	@SuppressWarnings("unchecked")
	private RowMapper<T> dataObjectCreator() {
		return (r, n) -> (T) helper().createDataObject(r);
	}

	private SqlProxyHelper helper() {
		return helperSupplier.get();
	}

	private static Atom<?> newInstance(String sql) {
		var atomSql = new AtomSql();
		return new Atom<>(atomSql, atomSql.helper(sql), true);
	}

	/**
	 * Atom結合用のインスタンスを生成します。<br>
	 * 生成されたインスタンスはスレッドセーフであり、static変数に保存し使用することが可能です。<br>
	 * このメソッドで生成されたインスタンスでは、検索等のデータベース操作を行うことはできません。<br>
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。
	 * @see IllegalAtomException
	 * @param creator
	 * @return creator内で生成されたインスタンス
	 */
	public static Atom<?> newStaticInstance(Function<AtomSql, Atom<?>> creator) {
		return creator.apply(new AtomSql(new Configure() {

			@Override
			public boolean enableLog() {
				throw new IllegalAtomException();
			}

			@Override
			public Pattern logStackTracePattern() {
				throw new IllegalAtomException();
			}
		}, new Endpoints(new Endpoint() {

			@Override
			public void batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
				throw new IllegalAtomException();
			}

			@Override
			public <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) {
				throw new IllegalAtomException();
			}

			@Override
			public int update(String sql, PreparedStatementSetter pss) {
				throw new IllegalAtomException();
			}

			@Override
			public void logSql(Log log, String originalSql, String sql, PreparedStatement ps) {
				throw new IllegalAtomException();
			}

			@Override
			public void bollowConnection(Consumer<ConnectionProxy> consumer) {
				throw new IllegalAtomException();
			}
		})));
	}

	/**
	 * 検索結果を{@link Stream}として返します。<br>
	 * 内部的に{@link ResultSet}を使用して逐次行取得しており、明示的に{@link Stream#close()}するまで{@link ResultSet}が閉じられないので注意が必要です。<br>
	 * 検索結果全件に対して操作を行いたい（{@link Stream#map}等）、結果オブジェクトすべてが必要でない場合は{@link List}で結果取得するよりも若干効率的です。
	 * @see AtomSql#tryStream(Runnable)
	 * @return {@DataObject}付与結果オブジェクトの{@link Stream}
	 */
	public Stream<T> stream() {
		return stream(dataObjectCreator());
	}

	/**
	 * 検索結果を{@link List}として返します。<br>
	 * 内部的に{@link ResultSet}から全件結果取得してから{@link List}として返却しています。<br>
	 * @return {@DataObject}付与結果オブジェクトの{@link List}
	 */
	public List<T> list() {
		return listAndClose(streamInternal(dataObjectCreator()));
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
	 * データオブジェクトの型を持たない{@link Atom}から、データオブジェクトの型を与えたインスタンスを新たに生成します。
	 * @param <R> {@link DataObject}が付与された型
	 * @param dataObjectClass {@link DataObject}が付与されたクラスオブジェクト
	 * @return データオブジェクトの型が与えられた新インスタンス
	 */
	public <R> Atom<R> apply(Class<R> dataObjectClass) {
		return new Atom<R>(
			atomSql,
			new SqlProxyHelper(
				helper(),
				Objects.requireNonNull(dataObjectClass)),
			andType);
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link Stream}として返します。
	 * @see #stream
	 * @see AtomSql#tryStream(Runnable)
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(RowMapper<R> mapper) {
		var stream = streamInternal(mapper);
		atomSql.registerStream(stream);
		return stream;
	}

	/**
	 * {@link RowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link RowMapper}
	 * @param <R> {@link RowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(RowMapper<R> mapper) {
		return listAndClose(streamInternal(mapper));
	}

	private <R> Stream<R> streamInternal(RowMapper<R> mapper) {
		Objects.requireNonNull(mapper);

		var helper = helper();

		var startNanos = System.nanoTime();
		try {
			return helper.entry.endpoint().queryForStream(helper.sql, helper, mapper);
		} finally {
			helper.logElapsed(startNanos);
		}
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link Stream}として返します。<br>
	 * @see #stream
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(SimpleRowMapper<R> mapper) {
		return stream((r, n) -> mapper.mapRow(r));
	}

	/**
	 * {@link SimpleRowMapper}により生成された結果オブジェクトを{@link List}として返します。<br>
	 * @see #list
	 * @param mapper {@link SimpleRowMapper}
	 * @param <R> {@link SimpleRowMapper}の生成した結果オブジェクトの型
	 * @return 結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(SimpleRowMapper<R> mapper) {
		return listAndClose(streamInternal((r, n) -> mapper.mapRow(r)));
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
		return helper().originalSql;
	}

	/**
	 * @see Atom#sql
	 * @return SQL文もしくはその一部
	 */
	@Override
	public String toString() {
		return helper().originalSql;
	}

	/**
	 * 内部に持つSQLが空文字列かどうかを返します。
	 * @return 内部に持つSQLが空文字列である場合、true
	 */
	public boolean isEmpty() {
		return helper().originalSql.isEmpty();
	}

	private <E> Optional<E> get(List<E> list) {
		if (list.size() > 1)
			//結果は1行以下でなければなりません
			throw new IllegalStateException("The result must be less than or equal to one row");

		return list.stream().findFirst();
	}

	/**
	 * 更新処理（INSERT, UPDATE, DELETE）のDML文、DDL文を実行します。<br>
	 * DDL、バッチ実行の場合、結果は常に0となります。
	 * @return 更新処理の場合、その結果件数
	 */
	public int update() {
		var helper = helper();

		var resources = atomSql.batchResources();
		if (resources == null) {//バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				return helper.entry.endpoint().update(helper.sql, helper);
			} finally {
				helper.logElapsed(startNanos);
			}
		}

		resources.put(helper.entry.name(), helper);

		return 0;
	}

	/**
	 * 内部に持つSQL文の一部同士を" "をはさんで文字列結合します。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。<br>
	 * 複数を一度に結合することが可能です。
	 * @param others 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> concat(Atom<?>... others) {
		var result = this;
		for (var another : others) {
			result = result.joinInternal(" ", another);
		}

		return result;
	}

	/**
	 * 内部に持つSQL文を、自身を先頭にdelimiterをはさんで文字列結合します。<br>
	 * このインスタンス及びもう一方の内部SQLは変化せず、結合された新たな{@link Atom}が返されます。<br>
	 * @param delimiter 区切り文字列
	 * @param others 結合対象
	 * @return 結合された新しい{@link Atom}
	 */
	public Atom<T> joinAndConcat(Atom<?> delimiter, Atom<?>... others) {
		var result = this;
		var delimiterString = delimiter.helper().sql;
		for (var another : others) {
			result = result.joinInternal(delimiterString, another);
		}

		return result;
	}

	private Atom<T> joinInternal(String delimiter, Atom<?> another) {
		Objects.requireNonNull(another);

		var helper = helper();
		var anotherHelper = another.helper();

		var sql = concat(delimiter, helper.sql, anotherHelper.sql);
		var originalSql = concat(delimiter, helper.originalSql, anotherHelper.originalSql);
		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql, originalSql, helper, anotherHelper),
			true);
	}

	/**
	 * 複数の{@link Atom}を、delimiterをはさんで文字列結合します。<br>
	 * 結合された{@link Atom}は、他の検索可能な{@link Atom}への結合に使用してください。
	 * @param delimiter 区切り文字
	 * @param members 結合対象
	 * @return 結合された{@link Atom}
	 */
	public static Atom<?> join(Atom<?> delimiter, List<Atom<?>> members) {
		return members.size() == 0
			? BLANK
			: members.get(0).joinAndConcat(delimiter, members.subList(1, members.size()).toArray(Atom<?>[]::new));
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで渡される{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>配列atomsのインデックス番号</i>}*&#47; です。<br>
	 * 例えば、展開対象1番目のための変数は /*${0}*&#47; と記述します。
	 * @param atoms 展開する{@link Atom}の配列
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(Atom<?>... atoms) {
		var helper = helper();
		var sql = helper.originalSql;

		var confidentials = new HashSet<String>(helper.confidentials);
		var argMap = new LinkedHashMap<String, Object>(helper.argMap);
		for (int i = 0; i < atoms.length; i++) {
			var atom = atoms[i];

			var pattern = Pattern.compile("/\\*\\$\\{" + i + "\\}\\*/");
			sql = pattern.matcher(sql).replaceAll(atom.helper().originalSql);

			var atomHelper = atom.helper();

			confidentials.addAll(atomHelper.confidentials);
			argMap.putAll(atomHelper.argMap);
		}

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql, confidentials, argMap, helper),
			true);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで回収する{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。
	 * @param atoms プレースホルダをキー、{@link Atom}を値として格納したマップ
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(Map<String, Atom<?>> atoms) {
		var helper = helper();
		var sql = new String[] { helper.originalSql };

		var confidentials = new HashSet<String>(helper.confidentials);
		var argMap = new LinkedHashMap<String, Object>(helper.argMap);

		atoms.entrySet().stream().forEach(e -> {
			var atom = e.getValue();

			var pattern = Pattern.compile("/\\*\\$\\{" + Objects.requireNonNull(e.getKey()) + "\\}\\*/");
			sql[0] = pattern.matcher(sql[0]).replaceAll(atom.helper().originalSql);

			var atomHelper = atom.helper();

			confidentials.addAll(atomHelper.confidentials);
			argMap.putAll(atomHelper.argMap);
		});

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql[0], confidentials, argMap, helper),
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

		var helper = helper();
		var anotherHelper = another.helper();

		var sqls = guardSql(andType, andTypeCurrent, helper);
		var anotherSqls = guardSql(another.andType, andTypeCurrent, anotherHelper);

		var sql = concat(delimiter, sqls.sql, anotherSqls.sql);
		var originalSql = concat(delimiter, sqls.originalSql, anotherSqls.originalSql);

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql, originalSql, helper, anotherHelper),
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
			return stream.toList();
		}
	}
}

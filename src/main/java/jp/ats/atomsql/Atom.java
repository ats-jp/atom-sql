package jp.ats.atomsql;

import java.lang.System.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
	 * ASC
	 */
	public static final Atom<?> ASC = newInstance("ASC");

	/**
	 * DESC
	 */
	public static final Atom<?> DESC = newInstance("DESC");

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

	/**
	 * AND
	 */
	public static final Atom<?> AND = newInstance("AND");

	/**
	 * OR
	 */
	public static final Atom<?> OR = newInstance("OR");

	private final AtomSql atomSql;

	private final Supplier<SqlProxyHelper> helperSupplier;

	private final Object nonThreadSafeHelperKey = new Object();

	private final boolean andType;

	Atom(AtomSql atomsql, SqlProxyHelper helper, boolean andType) {
		this.atomSql = atomsql;

		if (helper.sql.containsNonThreadSafeValue()) {
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
	 * 通常生成されたAtomインスタンスに結合するためだけに使用してください。<br>
	 * また、このメソッドを呼ぶ前に初期化が完了している必要があります。
	 * @see AtomSql#initialize(Configure)
	 * @see IllegalAtomException
	 * @param creator
	 * @return creator内で生成されたインスタンス
	 */
	public static Atom<?> newStaticInstance(Function<AtomSql, Atom<?>> creator) {
		return creator.apply(new AtomSql(new Endpoints(new Endpoint() {

			@Override
			public int[] batchUpdate(String sql, BatchPreparedStatementSetter bpss) {
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
			public void logSql(Logger logger, String originalSql, String sql, PreparedStatement ps) {
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
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #stream()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@DataObject}付与結果オブジェクトの{@link Stream}
	 */
	public <R> Stream<R> stream(Class<R> dataObjectClass) {
		return apply(dataObjectClass).stream();
	}

	/**
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #list()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@DataObject}付与結果オブジェクトの{@link List}
	 */
	public <R> List<R> list(Class<R> dataObjectClass) {
		return apply(dataObjectClass).list();
	}

	/**
	 * {@link DataObject}の型を強制的にセットし、検索結果を取得します。
	 * @see #get()
	 * @see #apply(Class)
	 * @param dataObjectClass データオブジェクトの型
	 * @return {@link DataObject}付与型の結果オブジェクト
	 * @throws IllegalStateException 検索結果が2件以上ある場合
	 */
	public <R> Optional<R> get(Class<R> dataObjectClass) {
		return apply(dataObjectClass).get();
	}

	/**
	 * データオブジェクトの型を持たない{@link Atom}から、データオブジェクトの型を与えたインスタンスを新たに生成します。
	 * @param <R> {@link DataObject}が付与された型
	 * @param dataObjectClass {@link DataObject}が付与されたクラスオブジェクト
	 * @return データオブジェクトの型が与えられた新インスタンス
	 */
	public <R> Atom<R> apply(Class<R> dataObjectClass) {
		if (dataObjectClass.getAnnotation(DataObject.class) == null)
			throw new IllegalStateException("dataObjectClass required annotation @" + DataObject.class.getSimpleName());

		return new Atom<R>(
			atomSql,
			new SqlProxyHelper(
				helper(),
				dataObjectClass),
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
			return helper.entry.endpoint().queryForStream(helper.sql.string(), helper, mapper);
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
		return helper().sql.string();
	}

	/**
	 * @see Atom#sql
	 * @return SQL文もしくはその一部
	 */
	@Override
	public String toString() {
		return helper().sql.originalString();
	}

	/**
	 * 内部に持つSQLが空文字列かどうかを返します。
	 * @return 内部に持つSQLが空文字列である場合、true
	 */
	public boolean isEmpty() {
		return helper().sql.isEmpty();
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
				return helper.entry.endpoint().update(helper.sql.string(), helper);
			} finally {
				helper.logElapsed(startNanos);
			}
		}

		resources.put(helper.entry.name(), helper, null, AtomSqlUtils.stackTrace());

		return 0;
	}

	/**
	 * 更新処理（INSERT, UPDATE, DELETE）のDML文、DDL文を実行します。<br>
	 * バッチ更新であっても処理結果件数はresultListenerに通知されます。<br>
	 * バッチ更新の場合、resultListener内で例外を投げると、未実行のバッチ更新が失われるため、途中から更新を再開することは出来ません。
	 * @param resultConsumer 
	 * @see AtomSql#tryBatch(Runnable)
	 * @see AtomSql#tryBatch(Supplier)
	 */
	public void update(Consumer<Integer> resultConsumer) {
		var helper = helper();

		var resources = atomSql.batchResources();
		if (resources == null) {//バッチ実行中ではない
			var startNanos = System.nanoTime();
			try {
				resultConsumer.accept(helper.entry.endpoint().update(helper.sql.string(), helper));

				return;
			} finally {
				helper.logElapsed(startNanos);
			}
		}

		resources.put(helper.entry.name(), helper, Objects.requireNonNull(resultConsumer), AtomSqlUtils.stackTrace());
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
			result = result.joinInternal(InnerSql.BLANK, another);
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
		var delimiterPart = delimiter.helper().sql;
		for (var another : others) {
			result = result.joinInternal(delimiterPart, another);
		}

		return result;
	}

	private Atom<T> joinInternal(InnerSql delimiter, Atom<?> another) {
		Objects.requireNonNull(another);

		var helper = helper();
		var anotherHelper = another.helper();

		var sql = concat(delimiter, helper.sql, anotherHelper.sql);
		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql, helper),
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

		var sql = helper.sql;
		for (int i = 0; i < atoms.length; i++) {
			var atom = atoms[i];

			var pattern = pattern(String.valueOf(i));
			sql = sql.put(pattern, atom.helper().sql);
		}

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql, helper),
			true);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで回収する{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。<br>
	 * キーワードのスペルミスを防ぎたい場合は、{@link Prototype}機能の使用を検討してください。
	 * @param keyword 変数名
	 * @param atom 展開する{@link Atom}
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(String keyword, Atom<?> atom) {
		var helper = helper();
		var sql = helper.sql;

		var pattern = pattern(Objects.requireNonNull(keyword));

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql.put(pattern, atom.helper().sql), helper),
			true);
	}

	/**
	 * この{@link Atom}の持つSQL文内のこのメソッド専用変数に、パラメータで回収する{@link Atom}のもつSQL文を展開します。<br>
	 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。<br>
	 * キーワードのスペルミスを防ぎたい場合は、{@link Prototype}機能の使用を検討してください。
	 * @param atoms 変数名をキー、{@link Atom}を値として格納したマップ
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(Map<String, Atom<?>> atoms) {
		var helper = helper();
		var sql = new InnerSql[] { helper.sql };

		atoms.entrySet().stream().forEach(e -> {
			var atom = e.getValue();

			var pattern = pattern(Objects.requireNonNull(e.getKey()));
			sql[0] = sql[0].put(pattern, atom.helper().sql);
		});

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(sql[0], helper),
			true);
	}

	private static Pattern pattern(String keyword) {
		return Pattern.compile("/\\*\\$\\{" + keyword + "\\}\\*/");
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
		return andOr(InnerSql.AND, another, true);
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
		return andOr(InnerSql.OR, another, andType);
	}

	private Atom<T> andOr(InnerSql delimiter, Atom<?> another, boolean andTypeCurrent) {
		Objects.requireNonNull(another);

		var helper = helper();
		var anotherHelper = another.helper();

		var sql = guardSql(andType, andTypeCurrent, helper);
		var anotherSql = guardSql(another.andType, andTypeCurrent, anotherHelper);

		return new Atom<T>(
			atomSql,
			new SqlProxyHelper(concat(delimiter, sql, anotherSql), helper),
			andTypeCurrent);
	}

	private static InnerSql concat(InnerSql delimiter, InnerSql sql1, InnerSql sql2) {
		if (sql1.isBlank()) return sql2;
		if (sql2.isBlank()) return sql1;

		return sql1.concat(delimiter).concat(sql2);
	}

	private static InnerSql guardSql(boolean andType, boolean andTypeCurrent, SqlProxyHelper helper) {
		if (!andType && andTypeCurrent) {//現在ORでAND追加された場合
			return helper.sql.isBlank() ? InnerSql.EMPTY : helper.sql.join("(", ")");
		}

		return helper.sql;
	}

	private static <T> List<T> listAndClose(Stream<T> stream) {
		try (stream) {
			return stream.toList();
		}
	}
}

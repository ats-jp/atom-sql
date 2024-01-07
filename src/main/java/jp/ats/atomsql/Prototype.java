package jp.ats.atomsql;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlProxy;

/**
 * {@link Atom}の持つSQL文に変数展開を行うための中間状態クラスです。<br>
 * {@link SqlProxy}のメソッドの戻り値としてこのクラスを使用した場合、SQL変数展開クラスの自動生成をアノテーションプロセッサに指示します。<br>
 * 指示は第二型パラメーターとして記述します。<br>
 * 記述可能なのはクラス名のみで、記述されたクラス名で{@link SqlProxy}と同一パッケージに、アノテーションプロセッサによりクラスが生成されます。<br>
 * 生成されたクラスには{@link SqlProxy}で指定されたSQL文から抽出された変数が、publicなフィールドとして作成されます。<br>
 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
 * キーワードにはJavaの識別子と同じ規則が適用されます。<br>
 * このアノテーションで指定するクラス名を（そのパッケージ内で）重複して指定してしまった場合、同じものを使用するのではなくコンパイルエラーとなります。
 * @author 千葉 哲嗣
 * @param <T> {@link DataObject}が付与された型
 * @param <A> 自動生成される変数展開用クラス
 */
public class Prototype<T, A> {

	private final Atom<T> atom;

	private final Class<?> atomsUnfolderClass;

	Prototype(Atom<T> atom, Class<?> atomsUnfolderClass) {
		this.atom = atom;
		this.atomsUnfolderClass = atomsUnfolderClass;
	}

	/**
	 * 自動生成された変数展開用クラス I をもとにSQL文の変数展開を行います。
	 * @see Atom#put(Map)
	 * @param consumer 変数展開用クラスのインスタンスを受け取る{@link Consumer}
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(Consumer<A> consumer) {
		Object instance;
		try {
			instance = atomsUnfolderClass.getConstructor().newInstance();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}

		@SuppressWarnings("unchecked")
		A atomsUnfolder = (A) instance;

		consumer.accept(atomsUnfolder);

		Map<String, Atom<?>> map = new HashMap<>();
		Arrays.stream(atomsUnfolderClass.getDeclaredFields()).forEach(f -> {
			f.setAccessible(true);

			try {
				var value = (Atom<?>) f.get(atomsUnfolder);

				if (value == null) return;

				map.put(f.getName(), value);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		});

		return atom.put(map);
	}

	/**
	 * 自動生成された変数展開用クラス I をもとにSQL文の変数展開を部分的に行います。<br>
	 * 展開された新しいインスタンスにさらに{@link #put(Consumer)}を行い完全に展開する必要があります。
	 * @see Prototype#put(Consumer)
	 * @param consumer 変数展開用クラスのインスタンスを受け取る{@link Consumer}
	 * @return 部分的に展開された新しい{@link Prototype}
	 */
	public Prototype<T, A> renew(Consumer<A> consumer) {
		return new Prototype<>(put(consumer), atomsUnfolderClass);
	}
}

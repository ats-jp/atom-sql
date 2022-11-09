package jp.ats.atomsql;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlInterpolation;

/**
 * {@link Atom}の持つSQL文に変数展開を行うための中間状態クラスです。
 * @author 千葉 哲嗣
 * @param <T> {@link DataObject}が付与された型
 * @param <I> 自動生成される変数展開用クラス
 */
public class HalfAtom<T, I> {

	private final Atom<T> atom;

	private final Class<?> sqlInterpolationClass;

	HalfAtom(Atom<T> atom, Class<?> sqlInterpolationClass) {
		this.atom = atom;
		this.sqlInterpolationClass = sqlInterpolationClass;
	}

	/**
	 * 自動生成された変数展開用クラス I をもとにSQL文の変数展開を行います。<br>
	 * 変数展開の詳しい内容は{@link SqlInterpolation}を参照してください。
	 * @see SqlInterpolation
	 * @see Atom#put(Map)
	 * @param consumer 変数展開用クラスのインスタンスを受け取る{@link Consumer}
	 * @return 展開された新しい{@link Atom}
	 */
	public Atom<T> put(Consumer<I> consumer) {
		Object instance;
		try {
			instance = sqlInterpolationClass.getConstructor().newInstance();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new IllegalStateException(e);
		}

		@SuppressWarnings("unchecked")
		var sqlInterpolation = (I) instance;

		consumer.accept(sqlInterpolation);

		Map<String, Atom<?>> map = new HashMap<>();
		Arrays.stream(sqlInterpolationClass.getDeclaredFields()).forEach(f -> {
			f.setAccessible(true);

			try {
				var value = (Atom<?>) f.get(sqlInterpolation);

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
	 * @see HalfAtom#put(Consumer)
	 * @param consumer 変数展開用クラスのインスタンスを受け取る{@link Consumer}
	 * @return 部分的に展開された新しい{@link HalfAtom}
	 */
	public HalfAtom<T, I> update(Consumer<I> consumer) {
		return new HalfAtom<>(put(consumer), sqlInterpolationClass);
	}
}

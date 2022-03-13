package jp.ats.atomsql;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.SourceVersion;

/**
 * 複数の{@link Atom}を回収するための入れ物クラスです。
 * @see Atom#inject(java.util.function.Consumer)
 * @author 千葉 哲嗣
 */
public class Atoms {

	final Map<String, Atom<?>> map = new HashMap<>();

	Atoms() {
	}

	/**
	 * この入れ物に{@link Atom}を追加します。<br>
	 * キーワードにはJavaの識別子と同じ規則が適用され、規則に反する場合には例外がスローされます。
	 * @param keyword SQL内の置換用キーワード
	 * @param atom 注入するSQL文の断片
	 * @throws IllegalStateException キーワードが規則に反する場合
	 */
	public void add(String keyword, Atom<?> atom) {
		Objects.requireNonNull(keyword);
		Objects.requireNonNull(atom);

		if (!SourceVersion.isIdentifier(keyword)) {
			throw new IllegalStateException("Invalid keyword [" + keyword + "]");
		}

		map.put(keyword, atom);
	}
}

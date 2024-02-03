package jp.ats.atomsql.processor;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import jp.ats.atomsql.Atom;

/**
 * 内部使用クラスです。<br>
 * SQL文から、{@link Atom}用変数を探します。<br>
 * プレースホルダは、Javaの識別子の規則に沿っている必要があります。
 * @author 千葉 哲嗣
 */
class AtomPlaceholderFinder {

	private static final Pattern pattern = Pattern.compile("\\$\\{([^\\s[\\p{Punct}&&[^_$]]]+)\\}");

	static String execute(String sql, Consumer<String> variableConsumer) {
		while (true) {
			var matcher = pattern.matcher(sql);

			if (!matcher.find())
				break;

			sql = sql.substring(matcher.end());

			var matched = matcher.group(1);

			if (!SourceVersion.isIdentifier(matched) || SourceVersion.isKeyword(matched)) continue;

			variableConsumer.accept(matched);
		}

		return sql;
	}
}

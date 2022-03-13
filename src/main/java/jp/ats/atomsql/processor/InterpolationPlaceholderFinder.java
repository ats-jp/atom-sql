package jp.ats.atomsql.processor;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import jp.ats.atomsql.annotation.SqlInterpolation;

/**
 * 内部使用クラスです。<br>
 * SQL文から、{@link SqlInterpolation}用プレースホルダを探します。<br>
 * プレースホルダは、Javaの識別子の規則に沿っている必要があります。
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class InterpolationPlaceholderFinder {

	private static final Pattern pattern = Pattern.compile("\\$\\{([^\\s[\\p{Punct}&&[^_$]]]+)\\}");

	public static String execute(String sql, Consumer<String> placeholderConsumer) {
		int position = 0;
		while (true) {
			var matcher = pattern.matcher(sql);

			if (!matcher.find())
				break;

			position = matcher.end();

			sql = sql.substring(position);

			var matched = matcher.group(1);

			if (!SourceVersion.isIdentifier(matched) || SourceVersion.isKeyword(matched)) continue;

			placeholderConsumer.accept(matched);
		}

		return sql;
	}
}

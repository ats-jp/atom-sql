package jp.ats.atomsql;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

/**
 * 内部使用クラスです。<br>
 * SQL文から、プレースホルダを探します。<br>
 * プレースホルダは、Javaの識別子の規則に沿っている必要があります。
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class PlaceholderFinder {

	private static final Pattern pattern = Pattern.compile(":([^\\s[\\p{Punct}&&[^_$]]]+)(?:/\\*([A-Z_]+)(?:<([A-Z_]+)>|)\\*/|)");

	public static String execute(String sql, Consumer<Found> placeholderConsumer) {
		int position = 0;
		while (true) {
			var matcher = pattern.matcher(sql);

			if (!matcher.find())
				break;

			var found = new Found();

			found.gap = sql.substring(0, matcher.start());

			position = matcher.end();

			sql = sql.substring(position);

			var matched = matcher.group(1);

			if (!SourceVersion.isIdentifier(matched) || SourceVersion.isKeyword(matched)) continue;

			found.placeholder = matched;

			found.typeHint = Optional.ofNullable(matcher.group(2));

			found.typeArgumentHint = Optional.ofNullable(matcher.group(3));

			placeholderConsumer.accept(found);
		}

		return sql;
	}

	public static class Found {

		public String gap;

		public String placeholder;

		public Optional<String> typeHint;

		public Optional<String> typeArgumentHint;
	}
}

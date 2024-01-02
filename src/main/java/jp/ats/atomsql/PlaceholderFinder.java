package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
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

	private static final Pattern pattern = Pattern.compile(":([^\\s[\\p{Punct}&&[^_$]]]+)(?:/\\*([^\\*<]+)(?:<([^\\*>]+)>|)\\*/|)");

	public static String execute(String sql, Consumer<Found> placeholderConsumer) {
		//誤検出分保管
		List<String> pseudoMatches = new LinkedList<>();
		while (true) {
			var matcher = pattern.matcher(sql);

			if (!matcher.find())
				break;

			var gap = sql.substring(0, matcher.start());

			var matched = matcher.group(1);

			var matchedAll = matcher.group();

			sql = sql.substring(matcher.end());

			if (!SourceVersion.isIdentifier(matched) || SourceVersion.isKeyword(matched)) {
				pseudoMatches.add(gap);
				pseudoMatches.add(matchedAll);
				continue;
			}

			var found = new Found();

			//溜まった誤検出分を追加
			if (pseudoMatches.size() > 0) {
				pseudoMatches.add(gap);
				gap = String.join("", pseudoMatches);
				pseudoMatches.clear();
			}

			found.gap = gap;

			found.all = matchedAll;

			found.placeholder = matched;

			found.typeHint = Optional.ofNullable(matcher.group(2));

			found.typeArgumentHint = Optional.ofNullable(matcher.group(3));

			placeholderConsumer.accept(found);
		}

		//溜まった誤検出分を追加
		if (pseudoMatches.size() > 0) {
			pseudoMatches.add(sql);
			return String.join("", pseudoMatches);
		}

		return sql;
	}

	public static class Found {

		public String gap;

		public String all;

		public String placeholder;

		public Optional<String> typeHint;

		public Optional<String> typeArgumentHint;
	}
}

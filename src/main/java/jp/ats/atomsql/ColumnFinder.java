package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

/**
 * 内部使用クラスです。<br>
 * SQL文から、カラム名を探します。<br>
 * カラム名はJavaの識別子として使用されるので、識別子の規則に沿っている必要があります。
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class ColumnFinder {

	private static final Pattern pattern = Pattern.compile(
		//:placeholderを排除するため、あえて:を含める
		"([^\\s[\\p{Punct}&&[^_:]]]+)(/\\*([^\\*]+)\\*/)",
		Pattern.CASE_INSENSITIVE);

	/**
	 * SQLからカラムの型ヒントを除去します。
	 * @param sql 元のSQL
	 * @return 型ヒント除去後のSQL
	 */
	static String normalize(String sql) {
		var list = new LinkedList<String>();
		list.add(execute(sql, f -> {
			list.add(f.gap);
			list.add(f.column);
		}));

		return String.join("", list);
	}

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

			if (matched.contains(":") //:placeHolder形式のものを除外、もちろん途中に:があってもNG
				|| !SourceVersion.isIdentifier(matched)
				|| SourceVersion.isKeyword(matched)
				|| AtomSqlUtils.isRestrictedKeyword(matched)) {
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

			found.column = matched;

			found.typeHint = Optional.ofNullable(matcher.group(3));

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

		public String column;

		public Optional<String> typeHint;
	}
}

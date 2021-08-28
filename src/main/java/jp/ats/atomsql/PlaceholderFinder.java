package jp.ats.atomsql;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class PlaceholderFinder {

	private static final Pattern pattern = Pattern.compile(":([a-zA-Z_$][a-zA-Z\\d_$]*)(?:/\\*([A-Z_]+)\\*/|)");

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

			found.placeholder = matcher.group(1);
			found.typeHint = Optional.ofNullable(matcher.group(2));

			placeholderConsumer.accept(found);
		}

		return sql;
	}

	public static class Found {

		public String gap;

		public String placeholder;

		public Optional<String> typeHint;
	}
}

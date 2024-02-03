package jp.ats.atomsql.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.regex.Pattern;

import jp.ats.atomsql.AtomSqlUtils;

/**
 * @author 千葉 哲嗣
 */
class Formatter {

	private static final Pattern pattern = Pattern.compile("\\[\\[([^\\]]+)\\]\\]", Pattern.MULTILINE + Pattern.DOTALL);

	static String format(String template, Map<String, String> arguments) {
		var buffer = new StringBuilder();

		var matcher = pattern.matcher(template);

		int start = 0;
		while (matcher.find()) {
			buffer.append(template.substring(start, matcher.start()));
			buffer.append(arguments.get(matcher.group(1)));
			start = matcher.end();
		}

		buffer.append(template.substring(start));

		return buffer.toString();
	}

	static String convertToTemplate(String source) {
		source = source.replaceAll("/\\*--\\*/.+?/\\*--\\*/", "");
		return source.replaceAll("/\\*\\+\\+(.+?)\\+\\+\\*/", "$1");
	}

	static String readTemplate(Class<?> target, String charset) {
		try (var input = target.getResourceAsStream(target.getSimpleName() + ".java")) {
			return new String(AtomSqlUtils.readBytes(input), charset);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	static String erase(String source, boolean erase) {
		if (erase)
			return source.replaceAll("/\\*--\\?--\\*/.+?/\\*--\\?--\\*/", "");

		return source.replaceAll("/\\*--\\?--\\*/", "");
	}

}

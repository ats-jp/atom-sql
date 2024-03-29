package jp.ats.atomsql;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class InnerSql {

	static final InnerSql EMPTY = new InnerSql("");

	static final InnerSql BLANK = new InnerSql(" ");

	static final InnerSql AND = new InnerSql(" AND ");

	static final InnerSql OR = new InnerSql(" OR ");

	static interface Element {

		void put(Pattern pattern, InnerSql another, List<Element> elements);

		void placeholder(Consumer<Placeholder> consumer);

		boolean hasNonThreadSafeValue();

		void appendTo(StringBuilder builder);

		void appendOriginalTo(StringBuilder builder);

		boolean isEmpty();

		boolean isBlank();
	}

	static record Text(String text) implements Element {

		@Override
		public void put(Pattern pattern, InnerSql another, List<Element> elements) {
			String remain = text;
			while (true) {
				var matcher = pattern.matcher(remain);

				if (!matcher.find())
					break;

				elements.add(new Text(remain.substring(0, matcher.start())));

				remain = remain.substring(matcher.end());

				elements.addAll(another.elements);
			}

			elements.add(new Text(remain));
		}

		@Override
		public boolean hasNonThreadSafeValue() {
			return false;
		}

		@Override
		public void placeholder(Consumer<Placeholder> consumer) {}

		@Override
		public void appendTo(StringBuilder builder) {
			builder.append(text);
		}

		@Override
		public void appendOriginalTo(StringBuilder builder) {
			builder.append(text);
		}

		@Override
		public boolean isEmpty() {
			return text.isEmpty();
		}

		@Override
		public boolean isBlank() {
			return text.isBlank();
		}
	}

	static record Placeholder(
		String name, //プレースホルダ名
		boolean confidential,
		String expression, //置換後のプレースホルダ
		String original, //元のプレースホルダ文字列全体（型ヒントを含む）
		AtomSqlType type, //型
		Object value, //値
		AtomSqlTypeFactory typeFactory//nonThreadSafe判定用
	) implements Element {

		@Override
		public void put(Pattern pattern, InnerSql another, List<Element> elements) {
			elements.add(this);
		}

		@Override
		public boolean hasNonThreadSafeValue() {
			return type.nonThreadSafe(value, typeFactory);
		}

		@Override
		public void placeholder(Consumer<Placeholder> consumer) {
			consumer.accept(this);
		}

		@Override
		public void appendTo(StringBuilder builder) {
			builder.append(expression);
		}

		@Override
		public void appendOriginalTo(StringBuilder builder) {
			builder.append(original);
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean isBlank() {
			return false;
		}
	}

	private final List<Element> elements;

	InnerSql(List<Element> elements) {
		this.elements = Collections.unmodifiableList(new LinkedList<>(elements));
	}

	InnerSql(String text) {
		this.elements = Collections.singletonList(new Text(text));
	}

	void placeholders(Consumer<Placeholder> consumer) {
		elements.forEach(e -> e.placeholder(consumer));
	}

	List<Placeholder> placeholders() {
		List<Placeholder> placeholders = new LinkedList<>();
		elements.forEach(e -> e.placeholder(placeholders::add));

		return placeholders;
	}

	String string() {
		var builder = new StringBuilder();
		elements.forEach(e -> e.appendTo(builder));

		return builder.toString();
	}

	String originalString() {
		var builder = new StringBuilder();
		elements.forEach(e -> e.appendOriginalTo(builder));

		return builder.toString();
	}

	InnerSql put(Pattern pattern, InnerSql another) {
		List<Element> elements = new LinkedList<>();
		this.elements.forEach(e -> e.put(pattern, another, elements));

		return new InnerSql(elements);
	}

	InnerSql concat(InnerSql another) {
		List<Element> elements = new LinkedList<>();
		elements.addAll(this.elements);
		elements.addAll(another.elements);

		return new InnerSql(elements);
	}

	InnerSql join(String prefix, String suffix) {
		List<Element> elements = new LinkedList<>();
		elements.add(new Text(prefix));
		elements.addAll(this.elements);
		elements.add(new Text(suffix));

		return new InnerSql(elements);
	}

	boolean isEmpty() {
		if (elements.size() == 0) return true;

		for (var e : elements) {
			if (!e.isEmpty()) return false;
		}

		return true;
	}

	boolean isBlank() {
		if (elements.size() == 0) return true;

		for (var e : elements) {
			if (!e.isBlank()) return false;
		}

		return true;
	}

	boolean containsNonThreadSafeValue() {
		return elements.stream().filter(e -> e.hasNonThreadSafeValue()).findFirst().isPresent();
	}

	@Override
	public String toString() {
		return string();
	}
}

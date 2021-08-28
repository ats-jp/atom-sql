package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author 千葉 哲嗣
 */
public class CommaSeparatedParameters<T> {

	private final List<T> values = new LinkedList<>();

	public CommaSeparatedParameters() {
	}

	public CommaSeparatedParameters(List<T> list) {
		this.values.addAll(list);
	}

	public CommaSeparatedParameters(Stream<T> stream) {
		stream.forEach(values::add);
	}

	public void add(T value) {
		values.add(value);
	}

	List<T> values() {
		return values;
	}
}

package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author 千葉 哲嗣
 * @param <T> 
 */
public class Csv<T> {

	private final List<T> values = new LinkedList<>();

	/**
	 * 
	 */
	public Csv() {
	}

	/**
	 * @param list
	 */
	public Csv(List<T> list) {
		this.values.addAll(list);
	}

	/**
	 * @param stream
	 */
	public Csv(Stream<T> stream) {
		stream.forEach(values::add);
	}

	/**
	 * @param value
	 */
	public void add(T value) {
		values.add(value);
	}

	List<T> values() {
		return values;
	}
}

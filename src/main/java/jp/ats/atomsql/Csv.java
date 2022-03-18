package jp.ats.atomsql;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import jp.ats.atomsql.annotation.NonThreadSafe;

/**
 * 条件で使用されるIN等、複数のSQLパラメータを操作するためのクラスです。<br>
 * 使用可能な型は{@link AtomSqlType}で定義されているものに限ります。<br>
 * このクラスのインスタンスはスレッドセーフです。<br>
 * そのスレッドセーフを維持するため、要素として保持できるのは{@link NonThreadSafe}を付与されていない{@link AtomSqlType}に限られます。
 * @see AtomSqlType
 * @author 千葉 哲嗣
 * @param <T> パラメータの型
 */
public class Csv<T> {

	/**
	 * listの内容を持つインスタンスを生成するメソッドです。
	 * @param list インスタンスが保持する値のリスト
	 * @return {@link Csv}
	 */
	public static <T> Csv<T> of(List<T> list) {
		return new Csv<>(list.stream());
	}

	/**
	 * streamの内容を持つインスタンスを生成するメソッドです。
	 * @param stream インスタンスが保持する値のストリーム
	 * @return {@link Csv}
	 */
	public static <T> Csv<T> of(Stream<T> stream) {
		return new Csv<>(stream);
	}

	/**
	 * valuesの内容を持つインスタンスを生成するメソッドです。
	 * @param values インスタンスが保持する値の配列
	 * @return {@link Csv}
	 */
	@SafeVarargs
	public static <T> Csv<T> of(T... values) {
		return new Csv<>(Arrays.stream(values));
	}

	private final List<T> values;

	private Csv(Stream<T> stream) {
		List<T> values = new LinkedList<>();
		stream.forEach(e -> checkAndAdd(e, values));

		this.values = Collections.unmodifiableList(values);
	}

	List<T> values() {
		return values;
	}

	private static <T> void checkAndAdd(T value, List<T> values) {
		//Csvの中にCsvは不可
		if (value instanceof Csv) throw new IllegalArgumentException("Csv cannot be used for Csv elements");

		var type = AtomSqlType.selectForPreparedStatement(value);

		if (type.nonThreadSafe()) throw new NonThreadSafeException();
		values.add(value);
	}
}

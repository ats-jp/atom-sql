package jp.ats.atomsql;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import jp.ats.atomsql.annotation.NonThreadSafe;

/**
 * 条件で使用されるIN等、複数のSQLパラメータを操作するためのクラスです。<br>
 * 使用可能な型は{@link AtomSqlType}で定義されているものに限ります。<br>
 * また、値から型を判定するため、値にnullは使用できません。<br>
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
		return new Csv<>(list);
	}

	/**
	 * streamの内容を持つインスタンスを生成するメソッドです。
	 * @param stream インスタンスが保持する値のストリーム
	 * @return {@link Csv}
	 */
	public static <T> Csv<T> of(Stream<T> stream) {
		return new Csv<>(stream.toList());
	}

	/**
	 * valuesの内容を持つインスタンスを生成するメソッドです。
	 * @param values インスタンスが保持する値の配列
	 * @return {@link Csv}
	 */
	@SafeVarargs
	public static <T> Csv<T> of(T... values) {
		return new Csv<>(Arrays.asList(values));
	}

	private final List<T> values;

	private Csv(List<T> values) {
		//値は空であってはなりません
		if (values.size() == 0) throw new IllegalArgumentException("Values must not be empty");

		//値にnullが含まれています
		if (values.stream().filter(v -> v == null).findFirst().isPresent()) throw new NullPointerException("Values contains null");

		this.values = Collections.unmodifiableList(values);
	}

	/**
	 * @return 内部で保持する値
	 */
	public List<T> values() {
		return values;
	}

	@Override
	public String toString() {
		return values.toString();
	}
}

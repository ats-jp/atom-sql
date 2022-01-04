package jp.ats.atomsql;

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

	private final List<T> values = new LinkedList<>();

	/**
	 * listの内容を持つインスタンスを生成するコンストラクタです。
	 * @param list インスタンスが保持する値のリスト
	 */
	public Csv(List<T> list) {
		list.forEach(this::checkAndAdd);
	}

	/**
	 * streamの内容を持つインスタンスを生成するコンストラクタです。
	 * @param stream インスタンスが保持する値のストリーム
	 */
	public Csv(Stream<T> stream) {
		stream.forEach(this::checkAndAdd);
	}

	List<T> values() {
		return values;
	}

	private void checkAndAdd(T value) {
		//Csvの中にCsvは不可
		if (value instanceof Csv) throw new IllegalArgumentException("Csv cannot be used for Csv elements");

		var type = AtomSqlType.selectForPreparedStatement(value);

		if (type.nonThreadSafe()) throw new NonThreadSafeException();
		values.add(value);
	}
}

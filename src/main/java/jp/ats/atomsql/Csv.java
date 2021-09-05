package jp.ats.atomsql;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 条件で使用されるIN等、複数のSQLパラメータを操作するためのクラスです。<br>
 * 使用可能な型は{@link AtomSqlType}で定義されているものに限ります。
 * @see AtomSqlType
 * @author 千葉 哲嗣
 * @param <T> パラメータの型
 */
public class Csv<T> {

	private final List<T> values = new LinkedList<>();

	/**
	 * 空の状態のインスタンスを生成するコンストラクタです。
	 */
	public Csv() {
	}

	/**
	 * listの内容を持つインスタンスを生成するコンストラクタです。
	 * @param list インスタンスが保持する値のリスト
	 */
	public Csv(List<T> list) {
		this.values.addAll(list);
	}

	/**
	 * streamの内容を持つインスタンスを生成するコンストラクタです。
	 * @param stream インスタンスが保持する値のストリーム
	 */
	public Csv(Stream<T> stream) {
		stream.forEach(values::add);
	}

	/**
	 * 値を追加します。
	 * @param value 値
	 */
	public void add(T value) {
		values.add(value);
	}

	List<T> values() {
		return values;
	}
}

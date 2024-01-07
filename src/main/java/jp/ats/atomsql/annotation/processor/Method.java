package jp.ats.atomsql.annotation.processor;

import java.util.function.Consumer;

import jp.ats.atomsql.Prototype;
import jp.ats.atomsql.annotation.SqlProxySupplier;

/**
 * @author 千葉 哲嗣
 */
public @interface Method {

	/**
	 * メソッド名
	 * @return メソッド名
	 */
	String name();

	/**
	 * メソッドの引数名
	 * @return メソッドの引数名
	 */
	String[] parameters();

	/**
	 * メソッドの引数の型
	 * @return メソッドの引数の型
	 */
	Class<?>[] parameterTypes();

	/**
	 * メソッドの{@link Consumer}に指定されたクラス
	 * @return メソッドの{@link Consumer}に指定されたクラス
	 */
	Class<?> parametersUnfolder() default Object.class;

	/**
	 * 戻り値の型パラメータで示されるAtom SQL検索結果で使用可能なクラス化または@link DataObject}クラス
	 * @return 戻り値の型パラメータで示されるAtom SQL検索結果で使用可能なクラス化または@link DataObject}クラス
	 */
	Class<?> result() default Object.class;

	/**
	 * {@link Prototype}に指定されたクラス
	 * @return {@link Prototype} に指定されたクラス
	 */
	Class<?> atomsUnfolder() default Object.class;

	/**
	 * {@link SqlProxySupplier}が付与されたメソッドの戻り値のクラス
	 * @return {@link SqlProxySupplier}が付与されたメソッドの戻り値のクラス
	 */
	Class<?> sqlProxy() default Object.class;
}

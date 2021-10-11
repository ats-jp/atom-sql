package jp.ats.atomsql.processor.annotation;

import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlParameters;

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
	 * {@link SqlParameters} に指定されたクラス
	 * @return {@link SqlParameters} に指定されたクラス
	 */
	Class<?> sqlParametersClass() default Object.class;

	/**
	 * 戻り値の型パラメータで示される {@link DataObject} クラス
	 * @return 戻り値の型パラメータで示される {@link DataObject} クラス
	 */
	Class<?> dataObjectClass() default Object.class;
}

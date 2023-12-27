package jp.ats.atomsql.annotation.processor;

import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlInterpolation;
import jp.ats.atomsql.annotation.SqlParameters;
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
	 * {@link SqlParameters}に指定されたクラス
	 * @return {@link SqlParameters} に指定されたクラス
	 */
	Class<?> sqlParameters() default Object.class;

	/**
	 * 戻り値の型パラメータで示される{@link DataObject}クラス
	 * @return 戻り値の型パラメータで示される {@link DataObject} クラス
	 */
	Class<?> dataObject() default Object.class;

	/**
	 * {@link SqlInterpolation}に指定されたクラス
	 * @return {@link SqlInterpolation} に指定されたクラス
	 */
	Class<?> sqlInterpolation() default Object.class;

	/**
	 * {@link SqlProxySupplier}が付与されたメソッドの戻り値のクラス
	 * @return {@link SqlProxySupplier}が付与されたメソッドの戻り値のクラス
	 */
	Class<?> sqlProxy() default Object.class;
}

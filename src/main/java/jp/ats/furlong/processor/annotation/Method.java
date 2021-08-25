package jp.ats.furlong.processor.annotation;

import jp.ats.furlong.DataObject;

/**
 * @author 千葉 哲嗣
 */
public @interface Method {

	/**
	 * メソッド名
	 * 
	 * @return メソッド名
	 */
	String name();

	/**
	 * メソッドの引数名
	 * 
	 * @return メソッドの引数名
	 */
	String[] args();

	/**
	 * メソッドの引数の型
	 * 
	 * @return メソッドの引数の型
	 */
	Class<?>[] argTypes();

	/**
	 * {@link SQLPrameter} に指定されたクラス
	 * 
	 * @return {@link SQLPrameter} に指定されたクラス
	 */
	Class<?> sqlParameterClass() default Object.class;

	/**
	 * 戻り値の型パラメータで示される {@link DataObject} クラス
	 * 
	 * @return 戻り値の型パラメータで示される {@link DataObject} クラス
	 */
	Class<?> dataObjectClass() default Object.class;
}

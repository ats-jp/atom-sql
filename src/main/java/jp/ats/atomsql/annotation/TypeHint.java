package jp.ats.atomsql.annotation;

import jp.ats.atomsql.AtomSqlType;

/**
 * 型ヒント
 * @author 千葉 哲嗣
 */
public @interface TypeHint {

	/**
	 * プレースホルダ名
	 * @return　プレースホルダ名
	 */
	String name();

	/**
	 * 型のヒント
	 * @return {@link AtomSqlType}実装クラスのクラス名
	 */
	String type();

	/**
	 * {@link AtomSqlType}の中で型パラメーターが存在するものを{@link #type()}で指定する場合の型引数です。
	 * @return {@link AtomSqlType}実装クラスのクラス名
	 */
	String typeArgument() default "";
}

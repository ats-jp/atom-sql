package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.type.ENUM;

/**
 * enumの各要素にデータベースと対応した値を紐づけるためのアノテーションです。<br>
 * 対象の{@link Enum}に{@link StringEnum}が付与されている必要があります。<br>
 * このアノテーションを付与しない場合、{@link Enum#name()}が代わりに使用されます。
 * @see StringEnum
 * @see ENUM
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface StringEnumValue {

	/**
	 * このアノテーションが持つ値を返します。
	 * @return データベースと対応した値
	 */
	String value();
}

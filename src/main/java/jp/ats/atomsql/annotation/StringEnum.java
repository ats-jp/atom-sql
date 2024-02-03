package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.type.ENUM;

/**
 * enumの各要素の値が{@link String}であることを表すアノテーションです。<br>
 * このアノテーションを付与した場合、{@link StringEnumValue}付与されている場合その値が使用され、付与されていない場合{Enum#name()}が値として使用されます。
 * @see ENUM
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface StringEnum {}

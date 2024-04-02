package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link DataObject}を、検索結果の違う複数のSELECT文で使用する場合など、SELECT句に対象となるカラムが存在しなかった場合、例外を発生させないようにします。
 * @author 千葉 哲嗣
 */
@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
public @interface OptionalColumn {}

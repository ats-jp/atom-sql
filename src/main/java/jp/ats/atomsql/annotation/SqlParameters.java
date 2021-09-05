package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 自動生成されるSQLパラメータの名前を示すアノテーションです。
 * @author 千葉 哲嗣
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface SqlParameters {

	/**
	 * 自動生成されるクラス名
	 * @return パッケージを除いたクラス名
	 */
	String value();
}

package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.Endpoint;

/**
 * {@link Endpoint}を登録する対象となる識別子を表します。
 * @author 千葉 哲嗣
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Qualifier {

	/**
	 * @return 識別子
	 */
	String value();
}

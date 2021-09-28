package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * {@link SqlProxy}を登録する対象となるJdbcTemplate識別子（{@link Qualifier}）を表します。
 * @author 千葉 哲嗣
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface JdbcTemplateName {

	/**
	 * @return JdbcTemplate識別子
	 */
	String value();
}

package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

import jp.ats.atomsql.AtomSqlInitializer;

/**
 * {@link SqlProxy}を登録する対象となるJdbcTemplate識別子（{@link Qualifier}）を表します。<br>
 * {@link SqlProxy}にこのアノテーションが付与された場合、{@link AtomSqlInitializer}のnameと一致しないとBeanとして登録されません。<br>
 * 正確には{@link SqlProxy}がBeanとして登録されるには<br>
 * このアノテーションを、{@link AtomSqlInitializer}生成時に渡したnameに一致する名称をvalueとして設定し、{@link SqlProxy}に付与<br>
 * このアノテーションを付与しない<br>
 * のいずれかとなります。
 * @see AtomSqlInitializer#AtomSqlInitializer(String, boolean)
 * @author 千葉 哲嗣
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface JdbcTemplateName {

	/**
	 * @return JdbcTemplate識別子
	 */
	String value();
}

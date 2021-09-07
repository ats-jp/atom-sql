package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.Atom;

/**
 * {@SqlProxy}に直接SQLを設定することが可能なアノテーションです。<br>
 * 完全なSQLである必要はなく、{@link Atom}を使用して結合等の操作が可能です。
 * @author 千葉 哲嗣
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface Sql {

	/**
	 * SQL文およびSQL文の断片です。<br>
	 * 必須です。
	 * @return SQL
	 */
	String value();
}

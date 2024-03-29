package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.AtomSql;

/**
 * {@link AtomSql}SQLログ出力の対象外であることを表すアノテーションです。<br>
 * @author 千葉 哲嗣
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface NoSqlLog {

	/**
	 * 経過時間のログ出力を行うかどうかを表すフラグです。<br>
	 * falseの場合、対象となるSQLのみログ出力を行わない場合と同等の動作となります。<br>
	 * デフォルトはtrueです。
	 * @return 行う場合、true
	 */
	boolean logElapseTime() default true;
}

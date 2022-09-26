package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.Atom;

/**
 * {@SqlProxy}にSQLファイルのパスを設定することが可能なアノテーションです。<br>
 * 完全なSQLである必要はなく、{@link Atom}を使用して結合等の操作が可能です。
 * @author 千葉 哲嗣
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SqlFile {

	/**
	 * SQL文を格納したファイル名です。<br>
	 * ファイルは、このアノテーションが付与されたメソッドの属する{@link SqlProxy}と同じパッケージに存在する必要があります。<br>
	 * この値を省略した場合、このアノテーションが付与されたメソッドの属する{@link SqlProxy}と同じパッケージに、ファイル名が<br>
	 * <code>SqlProxy名.メソッド名.sql</code><br>
	 * であるSQLファイルを使用します。<br>
	 * SqlProxy名は、SqlProxyクラスのバイナリ名を使用して下さい。
	 * @return SQL文を格納したファイル名
	 */
	String value() default "";
}

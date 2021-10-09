package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@link SqlProxy}のメソッドに付与可能な、機密情報を含む値を使用するSQLであることを表すアノテーションです。<br>
 * SQLにバインドされる値がパスワード等ログに出力したくない機密項目を含む場合、このアノテーションをそのメソッドに付与することでログにバインド値が出力されなくなります。
 * @author 千葉 哲嗣
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface ConfidentialSql {
}

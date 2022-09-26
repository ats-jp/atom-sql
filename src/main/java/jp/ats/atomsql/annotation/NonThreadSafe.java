package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.NonThreadSafeException;

/**
 * {@link SqlProxy}が付与されたクラスのメソッドにこのアノテーションを付与した場合、{@link AtomSqlType}のなかでこのアノテーションが付与された型を使用することが可能になります。<br>
 * 実行は{@link AtomSql#tryNonThreadSafe(Runnable)}か{@link AtomSql#tryNonThreadSafe(Supplier)}内でのみ可能です。
 * @see NonThreadSafeException
 * @see AtomSql#tryNonThreadSafe(Runnable)
 * @see AtomSql#tryNonThreadSafe(Supplier)
 * @author 千葉 哲嗣
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface NonThreadSafe {
}

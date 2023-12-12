package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.NonThreadSafeException;

/**
 * {@link AtomSqlType}の実装クラスのうち、このアノテーションが付与されている場合、その型は(内部の値の変更が可能である等の理由で)スレッドセーフではないことを表しています。<br>
 * 実行は{@link AtomSql#tryNonThreadSafe(Runnable)}か{@link AtomSql#tryNonThreadSafe(Supplier)}内でのみ可能です。
 * @see NonThreadSafeException
 * @see AtomSql#tryNonThreadSafe(Runnable)
 * @see AtomSql#tryNonThreadSafe(Supplier)
 * @author 千葉 哲嗣
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface NonThreadSafe {
}

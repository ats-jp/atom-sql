package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.AtomSql;

/**
 * {@SqlProxy}のメソッドにこのアノテーションを付与すると{@link AtomSql}を返すようになります。<br>
 * このアノテーションを付与するメソッドの戻り値は{AtomSql}型、パラメータ数は0である必要があります。
 * @author 千葉 哲嗣
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AtomSqlSupplier {
}

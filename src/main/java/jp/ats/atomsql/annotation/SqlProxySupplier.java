package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@SqlProxy}のメソッドにこのアノテーションを付与すると{@link SqlProxy}を返すようになります。<br>
 * このアノテーションを付与するメソッドの戻り値はvalueで指定した{SqlProxy}型、パラメータ数は0である必要があります。
 * @author 千葉 哲嗣
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SqlProxySupplier {
}

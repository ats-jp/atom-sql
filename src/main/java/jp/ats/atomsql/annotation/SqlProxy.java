package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.AtomSql;

/**
 * {@link AtomSql}のProxy作成対象となるインターフェイスであることを表すアノテーションです。<br>
 * {@link AtomSql}によって作成されたProxyインスタンスは、DB操作を行うことが可能になります。<br>
 * DB操作の実行にはSQL文が必要となり、{@link Sql}アノテーションによって設定されるか、または同一パッケージに<br>
 * クラス名.メソッド名.sql<br>
 * というファイル内にSQL文を記載することで利用可能となります。<br>
 * SQLにはパラメータという形で任意の値をセットすることが可能で、値のプレースホルダとして<br>
 * :プレースホルダ名<br>
 * という形式で設定可能です。<br>
 * <br>
 * プレースホルダ使用例<br>
 * <code>
 * SELECT * FROM customer WHERE customer_id = :customerId
 * </code>
 * <br>
 * <br>
 * プレースホルダへバインドする値は、メソッドのパラメータとして設定することが可能です。<br>
 * そのため、SQL内で使用しているプレースホルダ名と、メソッドのパラメータ名は一致している必要があります。<br>
 * また、SQL文内に多くのプレースホルダを使用する場合（INSERTのVALUES等）は{@link SqlParameters}の使用を検討してください。<br>
 * Proxyインターフェイスではdefaultメソッドを定義し使用することが可能ですが、注意点としてその場合Proxyインターフェイスをpublicにする必要があります。
 * @author 千葉 哲嗣
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SqlProxy {}

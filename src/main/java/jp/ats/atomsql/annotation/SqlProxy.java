package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlType;

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
 * <br>
 * また、SQL文内に多くのプレースホルダを使用する場合（INSERTのVALUES等）はSQLパラメータクラスの自動生成を行うことが可能です。<br>
 * SQLパラメータクラスは、{@link Consumer}の型パラメーターとして記述されることでアノテーションプロセッサに認識されます。<br>
 * 記述可能なのはクラス名のみで、記述されたクラス名で{@link SqlProxy}と同一パッケージに、アノテーションプロセッサによりクラスが生成されます。<br>
 * 生成されたクラスには{@link SqlProxy}で指定されたSQL文から抽出されたプレースホルダが、publicなフィールドとして作成されます。<br>
 * フィールドの型は、SQL内のプレースホルダ部分に型ヒントを記述することで設定することが可能です。<br>
 * 型ヒントの記述方法は":placeholder/*TYPE_HINT*&#047;"となり、TYPE_HINTには{@link AtomSqlType}で定義された列挙の名称のみが使用可能です。（フィールドをStringとしたい場合、型ヒントにSTRINGを記述）<br>
 * また、型ヒントをSQL内に記述したくない場合は、{@link TypeHints}を使用することで、そのパラメータとして型ヒントを設定することが可能となります。<br>
 * SQLパラメータクラスのクラス名を（そのパッケージ内で）重複して指定してしまった場合、同じものを使用するのではなくコンパイルエラーとなります。<br>
 * <br>
 * Proxyインターフェイスではdefaultメソッドを定義し使用することが可能ですが、注意点としてその場合Proxyインターフェイスをpublicにする必要があります。
 * @author 千葉 哲嗣
 * @see TypeHints
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SqlProxy {}

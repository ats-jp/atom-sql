package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlInitializer;

/**
 * {@link AtomSql} のProxy作成対象となるインターフェイスであることを表すアノテーションです。<br>
 * {@link AtomSql}によって作成されたProxyインスタンスは、内部で{@link JdbcTemplate}を使用してDB操作を行うことが可能になります。<br>
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
 * また、SQL文内に多くのプレースホルダを使用する場合（INSERTのVALUES等）は{@link SqlParameters}の使用を検討してください。
 * @author 千葉 哲嗣
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface SqlProxy {

	/**
	 * この{@link SqlProxy}を登録する対象となるJdbcTemplate識別子（{@link Qualifier}）を定義します。<br>
	 * {@link AtomSqlInitializer}生成時に渡したnameに一致する値を定義するか又は何も定義しない場合に、この{@link SqlProxy}がBeanとして登録されます。
	 * @see AtomSqlInitializer#AtomSqlInitializer(String, boolean)
	 * @return JdbcTemplate識別子
	 */
	String forName() default "";
}

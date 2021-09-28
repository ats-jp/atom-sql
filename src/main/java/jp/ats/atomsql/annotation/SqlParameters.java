package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.AtomSqlType;

/**
 * 自動生成されるSQLパラメータクラスのクラス名をアノテーションプロセッサに指示するためのアノテーションです。<br>
 * 指定可能なのはクラス名のみで、指定されたクラス名で{@link SqlProxy}と同一パッケージに、アノテーションプロセッサによりクラスが生成されます。<br>
 * 生成されたクラスには{@link SqlProxy}で指定されたSQL文から抽出されたプレースホルダが、publicなフィールドとして作成されます。<br>
 * フィールドの型は、SQL内のプレースホルダ部分に型ヒントを記述することで設定することが可能です。<br>
 * 型ヒントの記述方法は":placeholder/*TYPE_HINT*&#047;"となり、TYPE_HINTには{@link AtomSqlType}で定義された列挙の名称のみが使用可能です。（フィールドをStringとしたい場合、型ヒントにSTRINGを記述）<br>
 * このアノテーションで指定するクラス名を（そのパッケージ内で）重複して指定してしまった場合、同じものを使用するのではなくコンパイルエラーとなります。
 * @author 千葉 哲嗣
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface SqlParameters {

	/**
	 * 自動生成されるクラス名
	 * @return パッケージを除いたクラス名
	 */
	String value();
}
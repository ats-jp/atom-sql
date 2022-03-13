package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import jp.ats.atomsql.AtomSqlType;

/**
 * SQLパラメータクラスの自動生成をアノテーションプロセッサに指示するためのアノテーションです。<br>
 * 指示は{@link Consumer}の型パラメーターとして記述します。<br>
 * 記述可能なのはクラス名のみで、記述されたクラス名で{@link SqlProxy}と同一パッケージに、アノテーションプロセッサによりクラスが生成されます。<br>
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
	 * SQL内に同じプレースホルダが複数回記述しなければならない等、型ヒントを記述するのが煩雑な場合に型ヒントを直接与えます。
	 * @return　{@link TypeHint}配列
	 */
	TypeHint[] typeHints() default {};
}

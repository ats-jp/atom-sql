package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.atomsql.HalfAtom;

/**
 * SQL変数展開クラスの自動生成をアノテーションプロセッサに指示するためのアノテーションです。<br>
 * 指示は{@link HalfAtom}の第二型パラメーターとして記述します。<br>
 * 記述可能なのはクラス名のみで、記述されたクラス名で{@link SqlProxy}と同一パッケージに、アノテーションプロセッサによりクラスが生成されます。<br>
 * 生成されたクラスには{@link SqlProxy}で指定されたSQL文から抽出された変数が、publicなフィールドとして作成されます。<br>
 * 変数の書式は /*${<i>キーワード</i>}*&#47; です。<br>
 * キーワードにはJavaの識別子と同じ規則が適用されます。<br>
 * このアノテーションで指定するクラス名を（そのパッケージ内で）重複して指定してしまった場合、同じものを使用するのではなくコンパイルエラーとなります。
 * @author 千葉 哲嗣
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SqlInterpolation {
}

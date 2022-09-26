package jp.ats.atomsql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Stream;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.AtomSql;

/**
 * SELECT文の検索結果一行の内容を保持するためのデータクラスを表すアノテーションです。<br>
 * {@link SqlProxy}の検索結果を取得するための<br>
 * {@link Stream}<br>
 * {@link List}<br>
 * {@Optional}<br>
 * {@link Atom}<br>
 * の型パラメータとして使用できるのはこのアノテーションが付与されたクラスだけであり、その他のクラスを使用した{@link SqlProxy}はコンパイルエラーとなります。<br>
 * また、このアノテーションを付与したクラスは、特定のコンストラクタが一つだけ必要となり、実装するそのコンストラクタによって、{@link AtomSql}からの検索結果データの渡され方が変化します。<br>
 * パラメータなしコンストラクタのみ<br>
 * クラスに定義されたフィールドに、そのフィールド名を検索結果のカラム名として{@link ResultSet}から値を取り出し自動セットします。<br>
 * フィールドは、publicである必要があり、finalであってはいけません。<br>
 * パラメータが{@link ResultSet}一つのコンストラクタのみ<br>
 * インスタンス生成時に{@link ResultSet}が渡されます。<br>
 * 自身で{@link ResultSet}から値を取り出す必要があります。<br>
 * フィールドをfinalとして定義したい場合、こちらを選択してください。
 * @author 千葉 哲嗣
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface DataObject {
}

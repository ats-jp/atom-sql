package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Atom SQLで使用可能な型を表すインターフェイスです。
 * @author 千葉 哲嗣
 */
public interface AtomSqlType {

	/**
	 * この型に対応するJavaでの型を返します。
	 * @return type
	 */
	abstract Class<?> type();

	/**
	 * この型に合ったメソッドで{@link PreparedStatement}に値をセットします。
	 * @param index
	 * @param statement
	 * @param value
	 * @return 次index
	 */
	int bind(int index, PreparedStatement statement, Object value);

	/**
	 * この型に合ったメソッドで{@link ResultSet}から値を取得します。
	 * @param rs
	 * @param columnLabel
	 * @return 値
	 * @throws SQLException
	 */
	Object get(ResultSet rs, String columnLabel) throws SQLException;

	/**
	 * この型が型パラメータで使用される場合の代替型を返します。<br>
	 * 主にプリミティブな型がラッパークラスの型に変換するために使用します。
	 * @return {@link AtomSqlType}
	 */
	AtomSqlType toTypeArgument();

	/**
	 * この型がスレッドセーフではないかを返します。
	 * @return nonThreadSafeの場合、true
	 */
	boolean nonThreadSafe();

	/**
	 * SQL内で使用するプレースホルダ文字列を返します。
	 * @param value
	 * @return プレースホルダ文字列
	 */
	default String placeholderExpression(Object value) {
		return "?";
	}
}

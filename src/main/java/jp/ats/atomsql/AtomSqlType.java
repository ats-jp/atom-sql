package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.annotation.NonThreadSafe;
import jp.ats.atomsql.type.BIG_DECIMAL;
import jp.ats.atomsql.type.BINARY_STREAM;
import jp.ats.atomsql.type.BLOB;
import jp.ats.atomsql.type.BOOLEAN;
import jp.ats.atomsql.type.BYTE_ARRAY;
import jp.ats.atomsql.type.CHARACTER_STREAM;
import jp.ats.atomsql.type.CLOB;
import jp.ats.atomsql.type.CSV;
import jp.ats.atomsql.type.DATE;
import jp.ats.atomsql.type.DATETIME;
import jp.ats.atomsql.type.DOUBLE;
import jp.ats.atomsql.type.FLOAT;
import jp.ats.atomsql.type.INTEGER;
import jp.ats.atomsql.type.LONG;
import jp.ats.atomsql.type.OBJECT;
import jp.ats.atomsql.type.P_BOOLEAN;
import jp.ats.atomsql.type.P_DOUBLE;
import jp.ats.atomsql.type.P_FLOAT;
import jp.ats.atomsql.type.P_INT;
import jp.ats.atomsql.type.P_LONG;
import jp.ats.atomsql.type.STRING;
import jp.ats.atomsql.type.TIME;

/**
 * Atom SQLで使用可能な型を表すインターフェイスです。
 * @author 千葉 哲嗣
 */
public interface AtomSqlType {

	/**
	 * BIG_DECIMAL<br>
	 * 型ヒント用型名文字列
	 * @see BIG_DECIMAL
	 */
	public static final String BIG_DECIMAL = "BIG_DECIMAL";

	/**
	 * BINARY_STREAM<br>
	 * 型ヒント用型名文字列
	 * @see BINARY_STREAM
	 */
	public static final String BINARY_STREAM = "BINARY_STREAM";

	/**
	 * BLOB<br>
	 * 型ヒント用型名文字列
	 * @see BLOB
	 */
	public static final String BLOB = "BLOB";

	/**
	 * BOOLEAN<br>
	 * 型ヒント用型名文字列
	 * @see BOOLEAN
	 */
	public static final String BOOLEAN = "BOOLEAN";

	/**
	 * BYTE_ARRAY<br>
	 * 型ヒント用型名文字列
	 * @see BYTE_ARRAY
	 */
	public static final String BYTE_ARRAY = "BYTE_ARRAY";

	/**
	 * CHARACTER_STREAM<br>
	 * 型ヒント用型名文字列
	 * @see CHARACTER_STREAM
	 */
	public static final String CHARACTER_STREAM = "CHARACTER_STREAM";

	/**
	 * CLOB<br>
	 * 型ヒント用型名文字列
	 * @see CLOB
	 */
	public static final String CLOB = "CLOB";

	/**
	 * CSV<br>
	 * 型ヒント用型名文字列
	 * @see CSV
	 */
	public static final String CSV = "CSV";

	/**
	 * DATE<br>
	 * 型ヒント用型名文字列
	 * @see DATE
	 */
	public static final String DATE = "DATE";

	/**
	 * DATETIME<br>
	 * 型ヒント用型名文字列
	 * @see DATETIME
	 */
	public static final String DATETIME = "DATETIME";

	/**
	 * DOUBLE<br>
	 * 型ヒント用型名文字列
	 * @see DOUBLE
	 */
	public static final String DOUBLE = "DOUBLE";

	/**
	 * FLOAT<br>
	 * 型ヒント用型名文字列
	 * @see FLOAT
	 */
	public static final String FLOAT = "FLOAT";

	/**
	 * INTEGER<br>
	 * 型ヒント用型名文字列
	 * @see INTEGER
	 */
	public static final String INTEGER = "INTEGER";

	/**
	 * LONG<br>
	 * 型ヒント用型名文字列
	 * @see LONG
	 */
	public static final String LONG = "LONG";

	/**
	 * OBJECT<br>
	 * 型ヒント用型名文字列
	 * @see OBJECT
	 */
	public static final String OBJECT = "OBJECT";

	/**
	 * P_BOOLEAN<br>
	 * 型ヒント用型名文字列
	 * @see P_BOOLEAN
	 */
	public static final String P_BOOLEAN = "P_BOOLEAN";

	/**
	 * P_DOUBLE<br>
	 * 型ヒント用型名文字列
	 * @see P_DOUBLE
	 */
	public static final String P_DOUBLE = "P_DOUBLE";

	/**
	 * P_FLOAT<br>
	 * 型ヒント用型名文字列
	 * @see P_FLOAT
	 */
	public static final String P_FLOAT = "P_FLOAT";

	/**
	 * P_INT<br>
	 * 型ヒント用型名文字列
	 * @see P_INT
	 */
	public static final String P_INT = "P_INT";

	/**
	 * P_LONG<br>
	 * 型ヒント用型名文字列
	 * @see P_LONG
	 */
	public static final String P_LONG = "P_LONG";

	/**
	 * STRING<br>
	 * 型ヒント用型名文字列
	 * @see STRING
	 */
	public static final String STRING = "STRING";

	/**
	 * TIME<br>
	 * 型ヒント用型名文字列
	 * @see TIME
	 */
	public static final String TIME = "TIME";

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
	 * この型に合ったメソッドで{@link ResultSet}から値を取得します。
	 * @param rs
	 * @param columnIndex
	 * @return 値
	 * @throws SQLException
	 */
	Object get(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * この型が型パラメータで使用される場合の代替型を返します。<br>
	 * 主にプリミティブな型がラッパークラスの型に変換するために使用します。
	 * @return {@link AtomSqlType}
	 */
	AtomSqlType toTypeArgument();

	/**
	 * この型がスレッドセーフではないかを返します。<br>
	 * {@link CSV}等、実際の値を用いて判断するタイプのために実際に使用される値が提供されます。
	 * @param value 実際に使用される値
	 * @param factory 値の判定に使用する{@link AtomSqlTypeFactory}
	 * @return nonThreadSafeの場合、true
	 */
	default boolean nonThreadSafe(Object value, AtomSqlTypeFactory factory) {
		return getClass().getAnnotation(NonThreadSafe.class) != null;
	}

	/**
	 * SQL内で使用するプレースホルダ文字列表現を返します。
	 * @param value
	 * @return プレースホルダ文字列表現
	 */
	default String placeholderExpression(Object value) {
		return "?";
	}

	/**
	 * この型の文字列表現を返します。
	 * @return 型文字列表現
	 */
	default String typeExpression() {
		return type().getName();
	}

	/**
	 * この型が型パラメーターとして使用される際の文字列表現を返します。<br>
	 * 独自型を使用する時、サブクラスすべてを表現したい場合に<code>? extends SomeClass</code>のように使用できます。
	 * @return 型パラメーター文字列表現
	 */
	default String typeArgumentExpression() {
		return type().getName();
	}
}

package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.ats.atomsql.annotation.TypeHint;
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
	 * {@link TypeHint}用型名文字列
	 * @see BIG_DECIMAL
	 * @see TypeHint
	 */
	public static final String BIG_DECIMAL = "BIG_DECIMAL";

	/**
	 * BINARY_STREAM<br>
	 * {@link TypeHint}用型名文字列
	 * @see BINARY_STREAM
	 * @see TypeHint
	 */
	public static final String BINARY_STREAM = "BINARY_STREAM";

	/**
	 * BLOB<br>
	 * {@link TypeHint}用型名文字列
	 * @see BLOB
	 * @see TypeHint
	 */
	public static final String BLOB = "BLOB";

	/**
	 * BOOLEAN<br>
	 * {@link TypeHint}用型名文字列
	 * @see BOOLEAN
	 * @see TypeHint
	 */
	public static final String BOOLEAN = "BOOLEAN";

	/**
	 * BYTE_ARRAY<br>
	 * {@link TypeHint}用型名文字列
	 * @see BYTE_ARRAY
	 * @see TypeHint
	 */
	public static final String BYTE_ARRAY = "BYTE_ARRAY";

	/**
	 * CHARACTER_STREAM<br>
	 * {@link TypeHint}用型名文字列
	 * @see CHARACTER_STREAM
	 * @see TypeHint
	 */
	public static final String CHARACTER_STREAM = "CHARACTER_STREAM";

	/**
	 * CLOB<br>
	 * {@link TypeHint}用型名文字列
	 * @see CLOB
	 * @see TypeHint
	 */
	public static final String CLOB = "CLOB";

	/**
	 * CSV<br>
	 * {@link TypeHint}用型名文字列
	 * @see CSV
	 * @see TypeHint
	 */
	public static final String CSV = "CSV";

	/**
	 * DATE<br>
	 * {@link TypeHint}用型名文字列
	 * @see DATE
	 * @see TypeHint
	 */
	public static final String DATE = "DATE";

	/**
	 * DATETIME<br>
	 * {@link TypeHint}用型名文字列
	 * @see DATETIME
	 * @see TypeHint
	 */
	public static final String DATETIME = "DATETIME";

	/**
	 * DOUBLE<br>
	 * {@link TypeHint}用型名文字列
	 * @see DOUBLE
	 * @see TypeHint
	 */
	public static final String DOUBLE = "DOUBLE";

	/**
	 * FLOAT<br>
	 * {@link TypeHint}用型名文字列
	 * @see FLOAT
	 * @see TypeHint
	 */
	public static final String FLOAT = "FLOAT";

	/**
	 * INTEGER<br>
	 * {@link TypeHint}用型名文字列
	 * @see INTEGER
	 * @see TypeHint
	 */
	public static final String INTEGER = "INTEGER";

	/**
	 * LONG<br>
	 * {@link TypeHint}用型名文字列
	 * @see LONG
	 * @see TypeHint
	 */
	public static final String LONG = "LONG";

	/**
	 * OBJECT<br>
	 * {@link TypeHint}用型名文字列
	 * @see OBJECT
	 * @see TypeHint
	 */
	public static final String OBJECT = "OBJECT";

	/**
	 * P_BOOLEAN<br>
	 * {@link TypeHint}用型名文字列
	 * @see P_BOOLEAN
	 * @see TypeHint
	 */
	public static final String P_BOOLEAN = "P_BOOLEAN";

	/**
	 * P_DOUBLE<br>
	 * {@link TypeHint}用型名文字列
	 * @see P_DOUBLE
	 * @see TypeHint
	 */
	public static final String P_DOUBLE = "P_DOUBLE";

	/**
	 * P_FLOAT<br>
	 * {@link TypeHint}用型名文字列
	 * @see P_FLOAT
	 * @see TypeHint
	 */
	public static final String P_FLOAT = "P_FLOAT";

	/**
	 * P_INT<br>
	 * {@link TypeHint}用型名文字列
	 * @see P_INT
	 * @see TypeHint
	 */
	public static final String P_INT = "P_INT";

	/**
	 * P_LONG<br>
	 * {@link TypeHint}用型名文字列
	 * @see P_LONG
	 * @see TypeHint
	 */
	public static final String P_LONG = "P_LONG";

	/**
	 * STRING<br>
	 * {@link TypeHint}用型名文字列
	 * @see STRING
	 * @see TypeHint
	 */
	public static final String STRING = "STRING";

	/**
	 * TIME<br>
	 * {@link TypeHint}用型名文字列
	 * @see TIME
	 * @see TypeHint
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

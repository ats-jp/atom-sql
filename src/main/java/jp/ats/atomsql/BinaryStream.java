package jp.ats.atomsql;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.util.Objects;

/**
 * {@link PreparedStatement}に{@link InputStream}から読み込んだデータをセットする際に使用するクラスです。
 * @see PreparedStatement#setBinaryStream
 * @author 千葉 哲嗣
 */
public class BinaryStream {

	/**
	 * 入力
	 */
	public final InputStream input;

	/**
	 * 読み込むサイズ
	 */
	public final int length;

	/**
	 * 唯一のコンストラクタです。
	 * @param input 入力
	 * @param length 読み込むサイズ
	 */
	public BinaryStream(InputStream input, int length) {
		this.input = Objects.requireNonNull(input);
		this.length = length;
	}
}

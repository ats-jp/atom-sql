package jp.ats.atomsql;

import java.io.Reader;
import java.sql.PreparedStatement;
import java.util.Objects;

/**
 * {@link PreparedStatement}に{@link Reader}から読み込んだデータをセットする際に使用するクラスです。
 * @see PreparedStatement#setCharacterStream
 * @author 千葉 哲嗣
 */
public class CharacterStream {

	/**
	 * 入力
	 */
	public final Reader input;

	/**
	 * 読み込むサイズ
	 */
	public final int length;

	/**
	 * 唯一のコンストラクタです。
	 * @param input 入力
	 * @param length 読み込むサイズ
	 */
	public CharacterStream(Reader input, int length) {
		this.input = Objects.requireNonNull(input);
		this.length = length;
	}
}

package jp.ats.atomsql;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author 千葉 哲嗣
 */
public class BinaryStream {

	public final InputStream input;

	public final int length;

	public BinaryStream(InputStream input, int length) {
		this.input = Objects.requireNonNull(input);
		this.length = length;
	}
}

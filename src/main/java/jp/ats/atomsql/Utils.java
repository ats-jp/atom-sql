package jp.ats.atomsql;

import java.io.IOException;
import java.io.InputStream;

/**
 * 内部使用ユーティリティクラスです。
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class Utils {

	private static final byte[] BYTE_EMPTY_ARRAY = {};

	private static final int BUFFER_SIZE = 8192;

	public static String extractSimpleClassName(String className, String packageName) {
		var packageNameLength = packageName.length();
		return className.substring(packageNameLength == 0 ? 0 : packageNameLength + 1);
	}

	public static byte[] readBytes(InputStream in) throws IOException {
		byte[] concat = BYTE_EMPTY_ARRAY;
		byte[] b = new byte[BUFFER_SIZE];
		int readed;
		while ((readed = in.read(b, 0, BUFFER_SIZE)) > 0) {
			concat = concatByteArray(concat, concat.length, b, readed);
		}
		return concat;
	}

	private static byte[] concatByteArray(byte[] array1, int lengthof1, byte[] array2, int lengthof2) {
		byte[] concat = new byte[lengthof1 + lengthof2];
		System.arraycopy(array1, 0, concat, 0, lengthof1);
		System.arraycopy(array2, 0, concat, lengthof1, lengthof2);
		return concat;
	}
}

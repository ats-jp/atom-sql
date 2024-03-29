package jp.ats.atomsql;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 内部使用ユーティリティクラスです。
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public class AtomSqlUtils {

	private static final byte[] BYTE_EMPTY_ARRAY = {};

	private static final int BUFFER_SIZE = 8192;

	public static String extractSimpleClassName(String className, String packageName) {
		var packageNameLength = packageName.length();
		return className.substring(packageNameLength == 0 ? 0 : packageNameLength + 1);
	}

	public static byte[] readBytes(InputStream in) throws IOException {
		var concat = BYTE_EMPTY_ARRAY;
		var b = new byte[BUFFER_SIZE];
		int readed;
		while ((readed = in.read(b, 0, BUFFER_SIZE)) > 0) {
			concat = concatByteArray(concat, concat.length, b, readed);
		}

		return concat;
	}

	public static List<Class<?>> loadProxyClasses() throws IOException {
		List<Class<?>> result = new LinkedList<>();
		var enumeration = AtomSqlUtils.class.getClassLoader().getResources(Constants.PROXY_LIST);
		while (enumeration.hasMoreElements()) {
			result.addAll(loadProxyClasses(enumeration.nextElement()));
		}

		return result;
	}

	private static List<Class<?>> loadProxyClasses(URL url) throws IOException {
		try (var proxyList = url.openStream()) {
			if (proxyList == null) return Collections.emptyList();

			return Arrays.stream(new String(AtomSqlUtils.readBytes(proxyList), Constants.CHARSET).split("\\s+")).map(l -> {
				try {
					return Class.forName(l, false, Thread.currentThread().getContextClassLoader());
				} catch (ClassNotFoundException e) {
					//コンパイラの動作によっては削除されたクラスがまだ残っているかもしれないのでスキップ
					return null;
				}
			}).filter(c -> c != null).collect(Collectors.toList());
		}
	}

	static String toStringForBindingValue(Object v) {
		if (v == null) {
			return "null";
		} else if (v instanceof Number) {
			return v.toString();
		} else if (v instanceof byte[]) {
			return "byte array(" + ((byte[]) v).length + ")";
		}

		return "[" + v.toString() + "]";
	}

	private static byte[] concatByteArray(byte[] array1, int lengthof1, byte[] array2, int lengthof2) {
		var concat = new byte[lengthof1 + lengthof2];
		System.arraycopy(array1, 0, concat, 0, lengthof1);
		System.arraycopy(array2, 0, concat, lengthof1, lengthof2);
		return concat;
	}

	static Optional<StackTraceElement[]> stackTrace() {
		return AtomSql.configure().enableLog() ? Optional.of(new Throwable().getStackTrace()) : Optional.empty();
	}
}

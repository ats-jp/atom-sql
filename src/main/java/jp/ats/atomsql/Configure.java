package jp.ats.atomsql;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author 千葉 哲嗣
 */
public class Configure {

	/**
	 * シングルトンインスタンス
	 */
	public static final Configure instance = new Configure();

	private final String configFileName = "atom-sql.properties";

	/**
	 * SQLのログ出力を行う場合、true
	 */
	public final boolean enableLog;

	/**
	 * SQLログに含まれる呼び出し元情報のフィルタパターン<br>
	 * パターンにマッチしたものがログに出力される
	 */
	public final Pattern logStackTracePattern;

	private Configure() {
		var config = new Properties();

		try {
			var input = Configure.class.getClassLoader().getResourceAsStream(configFileName);

			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new IllegalStateException();
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "true"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));
	}
}

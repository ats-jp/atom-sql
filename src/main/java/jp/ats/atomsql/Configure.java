package jp.ats.atomsql;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author 千葉 哲嗣
 */
public class Configure {

	private final String configFileName = "atom-sql.properties";

	/**
	 * enable-log<br>
	 * SQLログを出力するかどうか<br>
	 * SQLのログ出力を行う場合、true
	 */
	public final boolean enableLog;

	/**
	 * log-stacktrace-pattern<br>
	 * SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）<br>
	 * パターンにマッチしたものがログに出力される
	 */
	public final Pattern logStackTracePattern;

	/**
	 * 指定されたパラメーターからインスタンスを作成します。
	 * @param enableLog SQLログを出力するかどうか
	 * @param logStackTracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
	 */
	public Configure(boolean enableLog, String logStackTracePattern) {
		this.enableLog = enableLog;

		if (logStackTracePattern == null || logStackTracePattern.isEmpty()) logStackTracePattern = ".+";
		this.logStackTracePattern = Pattern.compile(logStackTracePattern);
	}

	/**
	 * クラスパスのルートにあるatom-sql.propertiesから設定を読み込みインスタンスを作成します。
	 */
	public Configure() {
		var config = new Properties();

		try {
			var input = Configure.class.getClassLoader().getResourceAsStream(configFileName);

			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "false"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));
	}
}

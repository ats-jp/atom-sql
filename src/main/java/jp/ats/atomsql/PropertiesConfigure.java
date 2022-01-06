package jp.ats.atomsql;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author 千葉 哲嗣
 */
public class PropertiesConfigure implements Configure {

	private final String configFileName = "atom-sql.properties";

	/**
	 * enable-log<br>
	 * SQLログを出力するかどうか<br>
	 * SQLのログ出力を行う場合、true
	 */
	private final boolean enableLog;

	/**
	 * log-stacktrace-pattern<br>
	 * SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）<br>
	 * パターンにマッチしたものがログに出力される
	 */
	private final Pattern logStackTracePattern;

	/**
	 * クラスパスのルートにあるatom-sql.propertiesから設定を読み込みインスタンスを作成します。
	 */
	public PropertiesConfigure() {
		var config = new Properties();

		try {
			var input = PropertiesConfigure.class.getClassLoader().getResourceAsStream(configFileName);

			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "false"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));
	}

	@Override
	public boolean enableLog() {
		return enableLog;
	}

	@Override
	public Pattern logStackTracePattern() {
		return logStackTracePattern;
	}
}
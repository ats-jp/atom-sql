package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.Qualifier;

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
	 * use-qualifier<br>
	 * {@link Qualifier}を使用するかどうか
	 */
	private final boolean usesQualifier;

	private final AtomSqlTypeFactory typeFactory;

	/**
	 * クラスパスのルートにあるatom-sql.propertiesから設定を読み込みインスタンスを作成します。
	 */
	public PropertiesConfigure() {
		var config = new Properties();

		try (var input = PropertiesConfigure.class.getClassLoader().getResourceAsStream(configFileName)) {
			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "false"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));

		usesQualifier = Boolean.valueOf(config.getProperty("use-qualifier", "false"));

		var typeFactoryClass = config.getProperty("type-factory-class", null);

		if (typeFactoryClass == null) {
			typeFactory = DefaultAtomSqlTypeFactory.instance;
		} else {
			typeFactory = AtomSqlTypeFactory.newInstance(typeFactoryClass);
		}
	}

	@Override
	public boolean enableLog() {
		return enableLog;
	}

	@Override
	public Pattern logStackTracePattern() {
		return logStackTracePattern;
	}

	@Override
	public boolean usesQualifier() {
		return usesQualifier;
	}

	@Override
	public AtomSqlTypeFactory atomSqlTypeFactory() {
		return typeFactory;
	}
}

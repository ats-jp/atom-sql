package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.NoSqlLog;
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
	 * ignore-no-sql-log<br>
	 * アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか<br>
	 * 無視してSQLのログ出力を行う場合、true
	 */
	private final boolean ignoreNoSqlLog;

	/**
	 * use-qualifier<br>
	 * {@link Qualifier}を使用するかどうか
	 */
	private final boolean usesQualifier;

	/**
	 * type-factory-class<br>
	 * {@link AtomSqlTypeFactory}のFQCN
	 */
	private final String typeFactoryClass;

	/**
	 * batch-threshold<br>
	 * バッチ更新時の閾値<br>
	 * この値を超えるとバッチ実行され、件数がリセットされる<br>
	 * この値が0以下の場合、閾値はないものとして扱われる
	 */
	private final int batchThreshold;

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

		ignoreNoSqlLog = Boolean.valueOf(config.getProperty("ignore-no-sql-log", "false"));

		usesQualifier = Boolean.valueOf(config.getProperty("use-qualifier", "false"));

		typeFactoryClass = config.getProperty("type-factory-class", null);

		batchThreshold = Integer.parseInt(config.getProperty("batch-threshold", "0"));
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
	public boolean ignoreNoSqlLog() {
		return ignoreNoSqlLog;
	}

	@Override
	public boolean usesQualifier() {
		return usesQualifier;
	}

	@Override
	public String typeFactoryClass() {
		return typeFactoryClass;
	}

	@Override
	public int batchThreshold() {
		return batchThreshold;
	}
}

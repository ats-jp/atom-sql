package jp.ats.atomsql;

import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.Qualifier;

/**
 * Atom SQL用の設定をロードし、保持するクラスです。
 * @author 千葉 哲嗣
 */
public interface Configure {

	/**
	 * enable-log<br>
	 * SQLログを出力するかどうか<br>
	 * SQLのログ出力を行う場合、true
	 * @return SQLログを出力するかどうか
	 */
	boolean enableLog();

	/**
	 * log-stacktrace-pattern<br>
	 * SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）<br>
	 * パターンにマッチしたものがログに出力される
	 * @return フィルタパターン
	 */
	Pattern logStackTracePattern();

	/**
	 * {@link Qualifier}を使用するかどうか
	 * @return 使用する場合、true
	 */
	boolean useQualifier();
}

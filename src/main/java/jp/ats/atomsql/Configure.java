package jp.ats.atomsql;

import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.NoSqlLog;
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
	 * ignore-no-sql-log<br>
	 * アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか<br>
	 * 無視してSQLのログ出力を行う場合、true
	 * @return {@link NoSqlLog}を無視するか
	 */
	boolean ignoreNoSqlLog();

	/**
	 * use-qualifier<br>
	 * {@link Qualifier}を使用するかどうか
	 * @return 使用する場合、true
	 */
	boolean usesQualifier();

	/**
	 * type-factory-class<br>
	 * @return {@link AtomSqlTypeFactory}のFQCN
	 */
	String typeFactoryClass();

	/**
	 * batch-threshold<br>
	 * バッチ更新時の閾値<br>
	 * この値を超えるとバッチ実行され、件数がリセットされる<br>
	 * この値が0以下の場合、閾値はないものとして扱われる
	 * @return バッチ更新時の閾値
	 */
	int batchThreshold();
}

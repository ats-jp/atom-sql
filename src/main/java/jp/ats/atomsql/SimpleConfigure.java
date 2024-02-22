package jp.ats.atomsql;

import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.NoSqlLog;
import jp.ats.atomsql.annotation.Qualifier;

/**
 * Atom SQL用の設定を保持するレコードです。
 * @author 千葉 哲嗣
 * @param enableLog SQLログを出力するかどうか
 * @param logStackTracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
 * @param ignoreNoSqlLog アノテーション{@link NoSqlLog}が付与されていても、それを無視してSQLログを出力するかどうか
 * @param usesQualifier {@link Qualifier}を使用するかどうか
 * @param typeFactoryClass {@link AtomSqlTypeFactory}
 * @param batchThreshold バッチ更新時の閾値
 */
public record SimpleConfigure(
	boolean enableLog,
	Pattern logStackTracePattern,
	boolean ignoreNoSqlLog,
	boolean usesQualifier,
	String typeFactoryClass,
	int batchThreshold) implements Configure {

	/**
	 * スタブ設定
	 * @return スタブ設定
	 */
	public static Configure stub() {
		return new SimpleConfigure(false, null, false, false, null, 0);
	}
}

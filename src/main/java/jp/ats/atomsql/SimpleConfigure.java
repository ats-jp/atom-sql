package jp.ats.atomsql;

import java.util.regex.Pattern;

import jp.ats.atomsql.annotation.Qualifier;

/**
 * Atom SQL用の設定を保持するレコードです。
 * @author 千葉 哲嗣
 * @param enableLog SQLログを出力するかどうか
 * @param logStackTracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
 * @param useQualifier {@link Qualifier}を使用するかどうか
 */
public record SimpleConfigure(boolean enableLog, Pattern logStackTracePattern, boolean useQualifier) implements Configure {
}

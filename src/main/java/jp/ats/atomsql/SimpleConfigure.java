package jp.ats.atomsql;

import java.util.regex.Pattern;

/**
 * Atom SQL用の設定を保持するクラスです。
 * @author 千葉 哲嗣
 * @param enableLog SQLログを出力するかどうか
 * @param logStackTracePattern SQLログに含まれる呼び出し元情報のフィルタパターン（正規表現）
 */
public record SimpleConfigure(boolean enableLog, Pattern logStackTracePattern) implements Configure {
}

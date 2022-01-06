package jp.ats.atomsql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jp.ats.atomsql.annotation.SqlParameters;
import jp.ats.atomsql.annotation.SqlProxy;

/**
 * Atom SQL内部で使用される定数を保持するクラスです。
 * @author 千葉 哲嗣
 */
public interface Constants {

	/**
	 * {@link SqlProxy}メタ情報保持クラスの名称サフィックス
	 */
	public static final String METADATA_CLASS_SUFFIX = "$AtomSqlMetadata";

	/**
	 * SQLファイル、その他Atom SQLで使用する入出力ファイルの文字コード
	 */
	public static final Charset CHARSET = StandardCharsets.UTF_8;

	/**
	 * 改行コード
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * {@link SqlProxy}メタ情報保持クラスの一覧ファイル名
	 */
	public static final String PROXY_LIST = "jp.ats.atom-sql.proxy-list";

	/**
	 * {@link SqlParameters}自動生成クラスの一覧ファイル名
	 */
	public static final String PARAMETERS_LIST = "jp.ats.atom-sql.parameters-list";

	/**
	 * @{link ConfidentialSql}が付与されたSQL文のログ上の目印
	 */
	public static final String CONFIDENTIAL = "<<CONFIDENTIAL>>";
}

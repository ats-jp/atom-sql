package jp.ats.atomsql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author 千葉 哲嗣
 */
public interface Constants {

	/**
	 * 
	 */
	public static final String METADATA_CLASS_SUFFIX = "$AtomSqlMetadata";

	/**
	 * 
	 */
	public static final Charset CHARSET = StandardCharsets.UTF_8;

	/**
	 * 
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	/**
	 * 
	 */
	public static final String PROXY_LIST = "jp.ats.atom-sql.proxy-list";

	/**
	 * 
	 */
	public static final String PARAMETERS_LIST = "jp.ats.atom-sql.parameters-list";
}

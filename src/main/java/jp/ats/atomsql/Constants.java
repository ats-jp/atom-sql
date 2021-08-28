package jp.ats.atomsql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author 千葉 哲嗣
 */
public interface Constants {

	public static final String METADATA_CLASS_SUFFIX = "$AtomSqlMetadata";

	public static final Charset SQL_CHARSET = StandardCharsets.UTF_8;
}

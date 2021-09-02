package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author 千葉 哲嗣
 */
@SuppressWarnings("javadoc")
public interface SqlFileResolver {

	byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
		throws IOException;
}
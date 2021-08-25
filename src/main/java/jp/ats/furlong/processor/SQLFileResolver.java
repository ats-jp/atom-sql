package jp.ats.furlong.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface SQLFileResolver {

	byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
			throws IOException;
}

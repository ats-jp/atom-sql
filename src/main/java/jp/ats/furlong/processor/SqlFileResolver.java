package jp.ats.furlong.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface SqlFileResolver {

	byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
			throws IOException;
}

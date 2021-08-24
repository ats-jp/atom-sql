package jp.ats.furlong.processor;

import java.io.IOException;
import java.nio.file.Path;

public interface SQLFileResolver {

	byte[] resolve(Path classOutput, String packageName, String sqlFileName) throws IOException;
}

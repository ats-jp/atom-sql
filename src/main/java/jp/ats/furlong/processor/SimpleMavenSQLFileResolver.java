package jp.ats.furlong.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SimpleMavenSQLFileResolver implements SQLFileResolver {

	@Override
	public byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
			throws IOException {
		var projectRoot = classOutput.getParent().getParent();

		var packagePath = Paths.get(packageName.replace('.', '/'));

		var resources = projectRoot.resolve("src/main/java").resolve(packagePath).resolve(sqlFileName);

		if (Files.exists(resources)) {
			return Files.readAllBytes(resources);
		}

		var java = projectRoot.resolve("src/main/resources").resolve(packagePath).resolve(sqlFileName);

		if (Files.exists(java)) {
			return Files.readAllBytes(java);
		}

		return new byte[] {};
	}
}

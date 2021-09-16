package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author 千葉 哲嗣
 */
public class SimpleMavenSqlFileResolver implements SqlFileResolver {

	@Override
	public byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
		throws IOException, SqlFileNotFoundException {
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

		throw new SqlFileNotFoundException();
	}
}

package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.tools.StandardLocation;

/**
 * @author 千葉 哲嗣
 */
class ProcessorUtils {

	private static final PackageExtractor packageExtractor = new PackageExtractor();

	static boolean sameClass(TypeElement type, Class<?> clazz) {
		return type.getQualifiedName().toString().equals(clazz.getCanonicalName());
	}

	static PackageElement getPackageElement(TypeElement clazz) {
		Element enclosing = clazz;
		PackageElement packageElement = null;
		do {
			enclosing = enclosing.getEnclosingElement();
			packageElement = enclosing.accept(packageExtractor, null);
		} while (packageElement == null);

		return packageElement;
	}

	static Path getClassOutputPath(ProcessingEnvironment env) throws IOException {
		var classOutput = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "");
		return Paths.get(classOutput.toUri().toURL().toString().substring("file:/".length()));
	}

	private static class PackageExtractor extends SimpleElementVisitor14<PackageElement, Void> {

		@Override
		protected PackageElement defaultAction(Element e, Void p) {
			return null;
		}

		@Override
		public PackageElement visitPackage(PackageElement e, Void p) {
			return e;
		}
	}
}

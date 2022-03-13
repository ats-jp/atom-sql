package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.StandardLocation;

/**
 * @author 千葉 哲嗣
 */
class ProcessorUtils {

	static boolean sameClass(TypeElement type, Class<?> clazz) {
		return type.getQualifiedName().toString().equals(clazz.getCanonicalName());
	}

	static PackageElement getPackageElement(TypeElement clazz) {
		Element enclosing = clazz;
		PackageElement packageElement = null;
		do {
			enclosing = enclosing.getEnclosingElement();
			packageElement = enclosing.accept(PackageExtractor.instance, null);
		} while (packageElement == null);

		return packageElement;
	}

	static Path getClassOutputPath(ProcessingEnvironment env) throws IOException {
		var classOutput = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "");
		return Paths.get(classOutput.toUri().toURL().toString().substring("file:/".length()));
	}

	static record PackageNameAndBinaryClassName(String packageName, String binaryClassName) {
	}

	static PackageNameAndBinaryClassName getPackageNameAndBinaryClassName(Element method, ProcessingEnvironment env) {
		var clazz = method.getEnclosingElement().accept(TypeConverter.instance, null);

		PackageElement packageElement = getPackageElement(clazz);

		return new PackageNameAndBinaryClassName(
			packageElement.getQualifiedName().toString(),
			env.getElementUtils().getBinaryName(clazz).toString());
	}

	static ExecutableElement toExecutableElement(Element e) {
		return e.accept(MethodExtractor.instance, null);
	}

	static TypeMirror getTypeArgument(VariableElement p) {
		var types = p.asType().accept(TypeArgumentsExtractor.instance, null);
		if (types.size() == 0) return null;
		return types.get(0);
	}

	static Element toElement(TypeMirror type) {
		return type.accept(ElementConverter.instance, null);
	}

	private static class MethodExtractor extends SimpleElementVisitor14<ExecutableElement, Void> {

		private static final MethodExtractor instance = new MethodExtractor();

		@Override
		protected ExecutableElement defaultAction(Element e, Void p) {
			return null;
		}

		@Override
		public ExecutableElement visitExecutable(ExecutableElement e, Void p) {
			return e;
		}
	}

	private static class TypeArgumentsExtractor extends SimpleTypeVisitor14<List<? extends TypeMirror>, Void> {

		private static final TypeArgumentsExtractor instance = new TypeArgumentsExtractor();

		@Override
		protected List<? extends TypeMirror> defaultAction(TypeMirror e, Void p) {
			return null;
		}

		@Override
		public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void p) {
			return t.getTypeArguments();
		}

		@Override
		public List<? extends TypeMirror> visitError(ErrorType t, Void p) {
			return t.getTypeArguments();
		}
	}

	private static class ElementConverter extends SimpleTypeVisitor14<Element, Void> {

		private static ElementConverter instance = new ElementConverter();

		@Override
		protected Element defaultAction(TypeMirror e, Void p) {
			return DEFAULT_VALUE;
		}

		@Override
		public Element visitDeclared(DeclaredType t, Void p) {
			return t.asElement();
		}

		@Override
		public Element visitError(ErrorType t, Void p) {
			return t.asElement();
		}
	}

	private static class PackageExtractor extends SimpleElementVisitor14<PackageElement, Void> {

		private static final PackageExtractor instance = new PackageExtractor();

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

package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.StandardLocation;

import jp.ats.atomsql.AtomSql;

/**
 * @author 千葉 哲嗣
 */
class ProcessorUtils {

	static boolean sameClass(TypeElement type, Class<?> clazz) {
		return type.getQualifiedName().toString().equals(clazz.getCanonicalName());
	}

	static boolean containsSameClass(TypeElement type, Class<?>... classes) {
		var typeName = type.getQualifiedName().toString();
		return Arrays.stream(classes).filter(c -> typeName.equals(c.getCanonicalName())).findFirst().isPresent();
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

	/**
	 * クラス出力場所特定のための目印となるフラグファイル名
	 */
	private static final String flagFileName = AtomSql.class.getName() + ".flag";

	private static Path classOutputPath;

	static synchronized Path getClassOutputPath(ProcessingEnvironment env) throws IOException {
		//複数回getResource, createResourceするとエラーとなるので一度取得したパスを（コンパイル時なので）雑にキャッシュし利用する
		if (classOutputPath != null) return classOutputPath;

		//何かファイルを指定しないとエラーとなる（EclipseではOKだがMavenでのビルド時NG）ため、
		//しかたなくそれ専用のダミーファイルを指定（生成するわけではない）して取得する
		var flagFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", flagFileName);
		classOutputPath = Path.of(flagFile.toUri().toURL().toString().substring("file:/".length())).getParent();

		return classOutputPath;
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

	static List<? extends TypeMirror> getTypeArgument(Element p) {
		return p.asType().accept(TypeArgumentsExtractor.instance, null);
	}

	static List<? extends TypeMirror> getTypeArgument(TypeMirror p) {
		return p.accept(TypeArgumentsExtractor.instance, null);
	}

	static Element toElement(TypeMirror type) {
		return type.accept(ElementConverter.instance, null);
	}

	static TypeElement toTypeElement(Element e) {
		return e.accept(TypeConverter.instance, null);
	}

	private static class TypeConverter extends SimpleElementVisitor14<TypeElement, Void> {

		static final TypeConverter instance = new TypeConverter();

		private TypeConverter() {
		}

		@Override
		protected TypeElement defaultAction(Element e, Void p) {
			throw new ProcessException();
		}

		@Override
		public TypeElement visitType(TypeElement e, Void p) {
			return e;
		}
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

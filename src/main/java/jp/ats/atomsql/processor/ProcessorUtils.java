package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.UnknownElementException;
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

	static String getPackageName(TypeElement clazz) {
		Element enclosing = clazz;
		PackageElement packageElement = null;
		try {
			do {
				enclosing = enclosing.getEnclosingElement();
				packageElement = enclosing.accept(PackageExtractor.instance, null);
			} while (packageElement == null);
		} catch (UnknownElementException e) {
			return "";
		}

		return packageElement.getQualifiedName().toString();
	}

	/**
	 * クラス出力場所特定のための目印となるフラグファイル名
	 */
	private static final String flagFileName = AtomSql.class.getName() + ".flag";

	private static Path classOutputPath;

	/**
	 * 内部使用
	 * @param env
	 * @return {@link Path}
	 * @throws IOException
	 */
	static synchronized Path getClassOutputPath(ProcessingEnvironment env) throws IOException {
		//複数回getResource, createResourceするとエラーとなるので一度取得したパスを（コンパイル時なので）雑にキャッシュし利用する
		if (classOutputPath != null) return classOutputPath;

		//何かファイルを指定しないとエラーとなる（EclipseではOKだがMavenでのビルド時NG）ため、
		//しかたなくそれ専用のダミーファイルを指定（生成するわけではない）して取得する
		var flagFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", flagFileName);
		classOutputPath = Path.of(flagFile.toUri().toURL().toString().substring("file:/".length())).getParent();

		return classOutputPath;
	}

	/**
	 * 内部使用
	 * @param packageName 
	 * @param binaryClassName 
	 */
	static record PackageNameAndBinaryClassName(String packageName, String binaryClassName) {}

	/**
	 * 内部使用
	 * @param method
	 * @param env
	 * @return {@link PackageNameAndBinaryClassName}
	 */
	static PackageNameAndBinaryClassName getPackageNameAndBinaryClassName(Element method, ProcessingEnvironment env) {
		var clazz = method.getEnclosingElement().accept(TypeConverter.instance, null);

		return new PackageNameAndBinaryClassName(
			getPackageName(clazz),
			env.getElementUtils().getBinaryName(clazz).toString());
	}

	/**
	 * 内部使用
	 * @param e
	 * @return {@link ExecutableElement}
	 */
	static ExecutableElement toExecutableElement(Element e) {
		return e.accept(MethodExtractor.instance, null);
	}

	/**
	 * 内部使用
	 * @param p
	 * @return {@link TypeMirror}
	 */
	static List<? extends TypeMirror> getTypeArgument(Element p) {
		return p.asType().accept(TypeArgumentsExtractor.instance, null);
	}

	/**
	 * 内部使用
	 * @param p
	 * @return {@link TypeMirror}
	 */
	static List<? extends TypeMirror> getTypeArgument(TypeMirror p) {
		return p.accept(TypeArgumentsExtractor.instance, null);
	}

	/**
	 * 内部使用
	 * @param type
	 * @return {@link Element}
	 */
	static Element toElement(TypeMirror type) {
		return type.accept(ElementConverter.instance, null);
	}

	/**
	 * 内部使用
	 * @param e
	 * @return {@link TypeElement}
	 */
	static TypeElement toTypeElement(Element e) {
		return e.accept(TypeConverter.instance, null);
	}

	/**
	 * 内部使用
	 * @param type
	 * @return {@link TypeElement}
	 */
	static TypeElement toTypeElement(TypeMirror type) {
		return toTypeElement(toElement(type));
	}

	static String message(Exception e) {
		var message = e.getMessage();
		if (message == null || message.isBlank()) return e.getClass().getName();

		return message;
	}

	private static class TypeConverter extends SimpleElementVisitor14<TypeElement, Void> {

		static final TypeConverter instance = new TypeConverter();

		private TypeConverter() {}

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
			return Collections.emptyList();
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

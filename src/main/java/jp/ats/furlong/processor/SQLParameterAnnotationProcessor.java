package jp.ats.furlong.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.furlong.Constants;
import jp.ats.furlong.SQL;
import jp.ats.furlong.SQLParameter;

@SupportedAnnotationTypes("jp.ats.furlong.SQLParameter")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class SQLParameterAnnotationProcessor extends AbstractProcessor {

	private static final TypeConverter typeVisitor = new TypeConverter();

	private static final Class<?> DEFAULT_SQL_FILE_RESOLVER_CLASS = SimpleMavenSQLFileResolver.class;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			annotations.forEach(a -> {
				roundEnv.getElementsAnnotatedWith(a).forEach(e -> {
					var value = e.getAnnotation(SQLParameter.class).value();
					info(">>>>>>>>>>" + value);
					info("%%%%%%%%%%" + extractSQL(e));
				});
			});
		} catch (ProcessException e) {
			return false;
		}

		return true;
	}

	private static final PackageExtractor packageExtractor = new PackageExtractor();

	private String extractSQL(Element method) {
		var sql = method.getAnnotation(SQL.class);
		if (sql != null)
			return sql.value();

		var clazz = method.getEnclosingElement().accept(typeVisitor, null);

		Element enclosing = clazz;
		PackageElement packageElement = null;
		do {
			enclosing = enclosing.getEnclosingElement();
			packageElement = enclosing.accept(packageExtractor, null);
		} while (packageElement == null);

		var className = clazz.getQualifiedName().toString();

		var packageName = packageElement.getQualifiedName();

		int packageNameLength = packageName.length();

		var sqlFileName = className.substring(packageNameLength == 0 ? 0 : packageNameLength + 1) + "."
				+ method.getSimpleName() + ".sql";

		try {
			var classOutput = super.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "");
			var pathString = classOutput.toUri().toURL().toString();

			return new String(resolver(method).resolve(Paths.get(pathString.substring("file:/".length())),
					packageName.toString(), sqlFileName), Constants.SQL_CHARSET);
		} catch (IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	private final SQLFileResolver resolver(Element method) {
		var className = super.processingEnv.getOptions().get("sql-file-resolver");

		var clazz = DEFAULT_SQL_FILE_RESOLVER_CLASS;
		if (className != null) {
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				error("class [" + className + "] was not found", method);
				throw new ProcessException();
			}
		}

		try {
			return (SQLFileResolver) clazz.getConstructor().newInstance();
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
				| InstantiationException e) {
			error("error occurs while instantiation class [" + className + "]", method);
			throw new ProcessException();
		}
	}

	private static class PackageExtractor extends SimpleElementVisitor8<PackageElement, Void> {

		@Override
		protected PackageElement defaultAction(Element e, Void p) {
			return null;
		}

		@Override
		public PackageElement visitPackage(PackageElement e, Void p) {
			return e;
		}
	}

	private void info(String message) {
		super.processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	private void error(String message, Element e) {
		super.processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}
}

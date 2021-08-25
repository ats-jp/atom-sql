package jp.ats.furlong.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.furlong.Constants;
import jp.ats.furlong.ParameterType;
import jp.ats.furlong.PlaceholderFinder;
import jp.ats.furlong.SQL;
import jp.ats.furlong.SQLParameter;

@SupportedAnnotationTypes("jp.ats.furlong.SQLParameter")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class SQLParameterAnnotationProcessor extends AbstractProcessor {

	private static final Class<?> DEFAULT_SQL_FILE_RESOLVER_CLASS = SimpleMavenSQLFileResolver.class;

	private static final String newLine = System.getProperty("line.separator");

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			annotations.forEach(a -> {
				roundEnv.getElementsAnnotatedWith(a).forEach(e -> {
					var generateClassName = e.getAnnotation(SQLParameter.class).value();

					var clazz = e.getEnclosingElement().accept(TypeConverter.instance, null);

					PackageElement packageElement = ProcessorUtils.getPackageElement(clazz);

					var className = clazz.getQualifiedName().toString();
					var packageName = packageElement.getQualifiedName().toString();

					var fileName = packageName.isEmpty() ? generateClassName : packageName + "." + generateClassName;

					if (alreadyCreatedFiles.contains(fileName))
						return;

					var sql = extractSQL(packageName, className, e);

					String template = Formatter.readTemplate(SQLParameterTemplate.class, "UTF-8");
					template = Formatter.convertToTemplate(template);

					Map<String, String> param = new HashMap<>();

					param.put("PROCESSOR", SQLParameterAnnotationProcessor.class.getName());

					param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
					param.put("CLASS", generateClassName);

					var fields = new LinkedList<String>();
					PlaceholderFinder.execute(sql, f -> {
						var method = f.type.map(t -> ParameterType.valueOf(t)).orElse(ParameterType.OBJECT).type()
								.getName() + " " + f.placeholder + ";";
						fields.add(method);
					});

					param.put("FIELDS", String.join(newLine, fields));

					template = Formatter.format(template, param);

					try {
						try (Writer writer = super.processingEnv.getFiler().createSourceFile(fileName, e)
								.openWriter()) {
							writer.write(template);
						}
					} catch (IOException ioe) {
						error(ioe.getMessage(), e);
					}

					alreadyCreatedFiles.add(fileName);

					info(fileName + " generated");
				});
			});
		} catch (ProcessException e) {
			return false;
		}

		return true;
	}

	private String extractSQL(String packageName, String className, Element method) {
		var sql = method.getAnnotation(SQL.class);
		if (sql != null)
			return sql.value();

		var sqlFileName = ProcessorUtils.extractSimpleClassName(className, packageName) + "." + method.getSimpleName()
				+ ".sql";

		try {
			var classOutput = super.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "");
			var pathString = classOutput.toUri().toURL().toString();

			return new String(resolver(method).resolve(Paths.get(pathString.substring("file:/".length())),
					packageName.toString(), sqlFileName, super.processingEnv.getOptions()), Constants.SQL_CHARSET);
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

	private void info(String message) {
		super.processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	private void error(String message, Element e) {
		super.processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}
}

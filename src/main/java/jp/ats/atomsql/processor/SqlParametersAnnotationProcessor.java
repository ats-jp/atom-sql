package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.AtomSqlInitializer;
import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.AtomSqlUtils;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.PlaceholderFinder;
import jp.ats.atomsql.UnknownSqlTypeNameException;
import jp.ats.atomsql.annotation.SqlParameters;
import jp.ats.atomsql.annotation.TypeHint;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;
import jp.ats.atomsql.type.NULL;
import jp.ats.atomsql.type.OBJECT;

@SupportedAnnotationTypes("jp.ats.atomsql.annotation.SqlParameters")
@SupportedSourceVersion(SourceVersion.RELEASE_16)
@SuppressWarnings("javadoc")
public class SqlParametersAnnotationProcessor extends AbstractProcessor {

	private final AtomSqlTypeFactory typeFactory;

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	//生成クラス名, メソッド名
	//メソッドのパラメータの型はConsumer<SqlParameters>固定なので識別にはメソッド名だけでOK
	private final Map<String, MethodInfo> allParameters = new HashMap<>();

	private final MethodExtractor extractor = new MethodExtractor(() -> super.processingEnv);

	private static class MethodInfo {

		private final String parametersClass;

		private final String clazz;

		private final String method;

		private MethodInfo(String line) {
			var splitted = line.split("/");
			parametersClass = splitted[0];
			clazz = splitted[1];
			method = splitted[2];
		}

		private MethodInfo(String parametersClass, String clazz, String method) {
			this.parametersClass = parametersClass;
			this.clazz = clazz;
			this.method = method;
		}

		private String pack() {
			return parametersClass + "/" + clazz + "/" + method;
		}
	}

	static {
		AtomSqlInitializer.initializeIfUninitialized();
	}

	/**
	 * 
	 */
	public SqlParametersAnnotationProcessor() {
		typeFactory = AtomSqlTypeFactory.newInstance(AtomSqlInitializer.configure().atomSqlTypeFactoryClass());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			//他のクラスで作られた過去分を追加
			if (Files.exists(ProcessorUtils.getClassOutputPath(super.processingEnv).resolve(Constants.PARAMETERS_LIST))) {
				var listFile = super.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", Constants.PARAMETERS_LIST);
				try (var input = listFile.openInputStream()) {
					Arrays.stream(new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
						.map(l -> new MethodInfo(l))
						.forEach(i -> allParameters.put(i.parametersClass, i));
				}
			}
		} catch (IOException e) {
			super.processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
			return false;
		}

		annotations.forEach(a -> {
			roundEnv.getElementsAnnotatedWith(a).forEach(e -> {
				try {
					execute(e);
				} catch (UnknownSqlTypeNameException ustne) {
					error("Invalid type hint name [" + ustne.unknownTypeName() + "]", e);
				} catch (ProcessException pe) {
					//スキップして次の対象へ
				}
			});
		});

		var data = String.join(Constants.NEW_LINE, (allParameters.values().stream().map(i -> i.pack()).toList()));

		try {
			try (var output = new BufferedOutputStream(
				super.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Constants.PARAMETERS_LIST).openOutputStream())) {
				output.write(data.getBytes(Constants.CHARSET));
			}
		} catch (IOException e) {
			super.processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
			return false;
		}

		return true;
	}

	static String defaultSqlParametersClassName(String packageName, String className, String methodName) {
		//未指定の場合、クラス名_メソッド名とする
		return (className.substring(packageName.length()).replaceAll("^\\.", "") + "_" + methodName).replace("$", "_");
	}

	private static TypeElement toTypeElement(VariableElement e) {
		return ProcessorUtils.toTypeElement(ProcessorUtils.toElement(e.asType()));
	}

	private void execute(Element e) {
		var executable = ProcessorUtils.toExecutableElement(e);
		var parameters = executable.getParameters();

		if (parameters.size() != 1 || !ProcessorUtils.sameClass(toTypeElement(parameters.get(0)), Consumer.class)) {
			//メソッドeは、Consumerの1つのパラメータを必要とします
			error(
				"Method ["
					+ e.getSimpleName()
					+ "] requires one parameter of Consumer",
				e);

			return;
		}

		String generatePackageName;
		String generateClassName;
		{
			var p = parameters.get(0);

			var typeArgs = ProcessorUtils.getTypeArgument(p);
			if (typeArgs.size() == 0) {
				//Consumerと書かれた場合
				//Consumerは、型の引数を必要とします
				error("Consumer requires a type argument", p);
				return;
			}

			var typeArg = typeArgs.get(0);

			var element = ProcessorUtils.toElement(typeArg);
			if (element == null) {
				//Consumer<?>と書かれた場合
				//Consumerは、型の引数を必要とします
				error("Consumer requires a type argument", p);
				return;
			}

			var clazz = ProcessorUtils.toTypeElement(element);
			var className = clazz.getQualifiedName().toString();

			generatePackageName = ProcessorUtils.getPackageElement(clazz).getQualifiedName().toString();

			generateClassName = AtomSqlUtils.extractSimpleClassName(className, generatePackageName);
		}

		Result result;
		try {
			result = extractor.execute(e);
		} catch (SqlFileNotFoundException sfnfe) {
			//メソッドeにはSQLファイルが必要です
			error("Method " + e.getSimpleName() + " requires a SQL file", e);
			return;
		}

		var className = result.className;

		//パッケージはSqlProxyのあるパッケージ固定
		var packageName = result.packageName;

		var methodName = e.getSimpleName().toString();

		var newClassName = packageName.isEmpty() ? generateClassName : packageName + "." + generateClassName;

		var info = allParameters.get(newClassName);
		if (info != null && (!info.clazz.equals(className) || !info.method.equals(methodName))) {
			//generateClassNameという名前は既に他で使われています
			error("The name [" + generateClassName + "] has already been used elsewhere", e);
			return;
		}

		allParameters.put(newClassName, new MethodInfo(newClassName, className, methodName));

		if (alreadyCreatedFiles.contains(newClassName)) return;

		var sql = result.sql;

		var template = Formatter.readTemplate(SqlParametersTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("PROCESSOR", SqlParametersAnnotationProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("CLASS", generateClassName);

		var sqlParameters = e.getAnnotation(SqlParameters.class);

		Map<String, TypeHint> annotatedHints = new HashMap<>();
		Arrays.stream(sqlParameters.typeHints()).forEach(h -> {
			annotatedHints.put(h.name(), h);
		});

		var dubplicateChecker = new HashSet<String>();
		var fields = new LinkedList<String>();
		PlaceholderFinder.execute(sql, f -> {
			//重複は除外
			if (dubplicateChecker.contains(f.placeholder)) return;

			dubplicateChecker.add(f.placeholder);

			var annotatedHint = annotatedHints.get(f.placeholder);

			Optional<AtomSqlType> annotatedHintType;
			Optional<AtomSqlType> annotatedTypeArgument;
			if (annotatedHint != null) {
				annotatedHintType = Optional.of(typeFactory.typeOf(annotatedHint.type()));
				var arg = typeFactory.typeOf(annotatedHint.typeArgument());
				annotatedTypeArgument = arg == NULL.instance ? Optional.empty() : Optional.of(arg.toTypeArgument());
			} else {
				annotatedHintType = Optional.empty();
				annotatedTypeArgument = Optional.empty();
			}

			var typeArgument = annotatedTypeArgument.or(
				() -> f.typeArgumentHint.map(typeFactory::typeArgumentOf))
				.map(t -> "<" + t.typeArgumentExpression() + ">")
				.orElse("");

			var field = "public "
				+ annotatedHintType.orElseGet(() -> f.typeHint.map(typeFactory::typeOf).orElse(OBJECT.instance)).type().getName()
				+ typeArgument
				+ " "
				+ f.placeholder
				+ ";";
			fields.add(field);
		});

		param.put("FIELDS", String.join(Constants.NEW_LINE, fields));

		template = Formatter.format(template, param);

		try {
			try (var output = new BufferedOutputStream(super.processingEnv.getFiler().createSourceFile(newClassName, e).openOutputStream())) {
				output.write(template.getBytes(Constants.CHARSET));
			}
		} catch (IOException ioe) {
			error(ioe.getMessage(), e);
		}

		alreadyCreatedFiles.add(newClassName);
	}

	private void error(String message, Element e) {
		super.processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}
}

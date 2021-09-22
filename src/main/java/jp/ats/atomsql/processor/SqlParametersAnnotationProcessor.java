package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.PlaceholderFinder;
import jp.ats.atomsql.Utils;
import jp.ats.atomsql.annotation.SqlParameters;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

@SupportedAnnotationTypes("jp.ats.atomsql.annotation.SqlParameters")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SuppressWarnings("javadoc")
public class SqlParametersAnnotationProcessor extends AbstractProcessor {

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

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			//他のクラスで作られた過去分を追加
			if (Files.exists(ProcessorUtils.getClassOutputPath(super.processingEnv).resolve(Constants.PARAMETERS_LIST))) {
				var listFile = super.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", Constants.PARAMETERS_LIST);
				try (var input = listFile.openInputStream()) {
					Arrays.stream(new String(Utils.readBytes(input), Constants.CHARSET).split("\\s+"))
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
				} catch (ProcessException pe) {
					//スキップして次の対象へ
				}
			});
		});

		var data = String.join(Constants.NEW_LINE, (allParameters.values().stream().map(i -> i.pack()).collect(Collectors.toList())));

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

	private void execute(Element e) {
		var generateClassName = e.getAnnotation(SqlParameters.class).value();

		if (generateClassName.isBlank()) {
			error("value of " + SqlParameters.class.getSimpleName() + " is blank", e);
			return;
		}

		Result result;
		try {
			result = extractor.execute(e);
		} catch (SqlFileNotFoundException sfnfe) {
			error("method " + e.getSimpleName() + " needs SQL file", e);
			return;
		}

		var className = result.className;
		var packageName = result.packageName;

		var newClassName = packageName.isEmpty() ? generateClassName : packageName + "." + generateClassName;

		var info = allParameters.get(newClassName);
		var methodName = e.getSimpleName().toString();
		if (info != null && (!info.clazz.equals(className) || !info.method.equals(methodName))) {
			error("duplicate name [" + generateClassName + "]", e);
			return;
		}

		allParameters.put(newClassName, new MethodInfo(newClassName, className, methodName));

		if (alreadyCreatedFiles.contains(newClassName)) return;

		var sql = result.sql;

		String template = Formatter.readTemplate(SqlParametersTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("PROCESSOR", SqlParametersAnnotationProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("CLASS", generateClassName);

		var fields = new LinkedList<String>();
		PlaceholderFinder.execute(sql, f -> {
			var typeArgument = f.typeArgumentHint.map(t -> "<" + AtomSqlType.safeTypeArgumentValueOf(t).type().getName() + ">").orElse("");

			var method = "public "
				+ f.typeHint.map(t -> AtomSqlType.safeValueOf(t)).orElse(AtomSqlType.OBJECT).type().getName()
				+ typeArgument
				+ " "
				+ f.placeholder
				+ ";";
			fields.add(method);
		});

		param.put("FIELDS", String.join(Constants.NEW_LINE, fields));

		template = Formatter.format(template, param);

		try {
			try (Writer writer = super.processingEnv.getFiler().createSourceFile(newClassName, e).openWriter()) {
				writer.write(template);
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

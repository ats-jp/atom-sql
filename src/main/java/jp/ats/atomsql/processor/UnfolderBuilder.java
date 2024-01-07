package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.AtomSqlUtils;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.MethodExtractor.SqlNotFoundException;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

/**
 * @author 千葉 哲嗣
 */
abstract class UnfolderBuilder {

	private final Supplier<ProcessingEnvironment> processingEnv;

	private final DuplicateClassChecker checker;

	private final MethodExtractor extractor;

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	UnfolderBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		this.processingEnv = processingEnv;
		this.checker = checker;
		extractor = new MethodExtractor(processingEnv);
	}

	static class DuplicateClassChecker {

		/**
		 * 生成したクラスの一覧ファイル名
		 */
		private static final String fileName = "jp.ats.atom-sql.unfolder-list";

		//生成クラス名, メソッド名
		//メソッドのパラメータの型はConsumer<Unfolder>固定なので識別にはメソッド名だけでOK
		private final Map<String, MethodInfo> allUnfolderNames = new HashMap<>();

		void start(ProcessingEnvironment env) {
			try {
				//他のクラスで作られた過去分を追加
				if (Files.exists(ProcessorUtils.getClassOutputPath(env).resolve(fileName))) {
					var listFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", fileName);
					try (var input = listFile.openInputStream()) {
						Arrays.stream(new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
							.filter(l -> l.length() > 0)//空の場合スキップ
							.map(l -> new MethodInfo(l))
							.forEach(i -> allUnfolderNames.put(i.generatedClass, i));
					}
				}
			} catch (IOException e) {
				env.getMessager().printMessage(Kind.ERROR, e.getMessage());
				return;
			}
		}

		void finish(ProcessingEnvironment env) {
			var data = String.join(Constants.NEW_LINE, (allUnfolderNames.values().stream().map(i -> i.pack()).toList()));

			try (var output = new BufferedOutputStream(
				env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName).openOutputStream())) {
				output.write(data.getBytes(Constants.CHARSET));
			} catch (IOException e) {
				env.getMessager().printMessage(Kind.ERROR, e.getMessage());
			}
		}

		private void put(String className, MethodInfo info) {
			allUnfolderNames.put(className, info);
		}

		private MethodInfo get(String className) {
			return allUnfolderNames.get(className);
		}
	}

	void execute(ExecutableElement method) {
		var result = extractTargetElement(method);

		if (!result.success) return;

		execute(method, result.targetType);
	}

	abstract ExtractResult extractTargetElement(ExecutableElement method);

	static record ExtractResult(boolean success, Element targetType) {

		static final ExtractResult fail = new ExtractResult(false, null);
	}

	abstract List<String> fields(ExecutableElement method, String sql);

	void execute(ExecutableElement method, Element targetType) {
		String generatePackageName;
		String generateClassName;
		{
			var clazz = ProcessorUtils.toTypeElement(targetType);
			var className = clazz.getQualifiedName().toString();

			generatePackageName = ProcessorUtils.getPackageName(clazz);

			generateClassName = AtomSqlUtils.extractSimpleClassName(className, generatePackageName);
		}

		Result result;
		try {
			result = extractor.execute(method);
		} catch (SqlNotFoundException | SqlFileNotFoundException ex) {
			error(ex.getMessage(), method);
			return;
		}

		var className = result.className;

		//パッケージはSqlProxyのあるパッケージ固定
		var packageName = result.packageName;

		var methodName = method.getSimpleName().toString();

		var newClassName = packageName.isEmpty() ? generateClassName : packageName + "." + generateClassName;

		if (alreadyCreatedFiles.contains(newClassName)) return;

		var info = checker.get(newClassName);
		if (info != null && (!info.enclosingClass.equals(className) || !info.method.equals(methodName))) {
			//generateClassNameという名前は既に他で使われています
			error("The name [" + generateClassName + "] has already been used elsewhere", method);
			return;
		}

		checker.put(newClassName, new MethodInfo(newClassName, className, methodName));

		var template = Formatter.readTemplate(UnfolderTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("CLASS", generateClassName);

		param.put("FIELDS", String.join(Constants.NEW_LINE, fields(method, result.sql)));

		template = Formatter.format(template, param);

		try {
			try (var output = new BufferedOutputStream(processingEnv.get().getFiler().createSourceFile(newClassName, method).openOutputStream())) {
				output.write(template.getBytes(Constants.CHARSET));
			}

			alreadyCreatedFiles.add(newClassName);
		} catch (IOException ioe) {
			error(ioe.getMessage(), method);
		}
	}

	void error(String message, Element e) {
		processingEnv.get().getMessager().printMessage(Kind.ERROR, message, e);
	}
}

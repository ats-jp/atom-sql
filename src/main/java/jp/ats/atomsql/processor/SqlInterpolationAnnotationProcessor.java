package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
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
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.AtomSqlInitializer;
import jp.ats.atomsql.AtomSqlUtils;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.HalfAtom;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes("jp.ats.atomsql.annotation.SqlInterpolation")
@SupportedSourceVersion(SourceVersion.RELEASE_16)
public class SqlInterpolationAnnotationProcessor extends AbstractProcessor {

	private final MethodExtractor extractor = new MethodExtractor(() -> super.processingEnv);

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	//生成クラス名, メソッド名
	private final Map<String, MethodInfo> allInterpolations = new HashMap<>();

	static {
		AtomSqlInitializer.initializeIfUninitialized();
	}

	private static class MethodInfo {

		private final String intepolationClass;

		private final String clazz;

		private final String method;

		private MethodInfo(String line) {
			var splitted = line.split("/");
			intepolationClass = splitted[0];
			clazz = splitted[1];
			method = splitted[2];
		}

		private MethodInfo(String parametersClass, String clazz, String method) {
			this.intepolationClass = parametersClass;
			this.clazz = clazz;
			this.method = method;
		}

		private String pack() {
			return intepolationClass + "/" + clazz + "/" + method;
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			//他のクラスで作られた過去分を追加
			if (Files.exists(ProcessorUtils.getClassOutputPath(super.processingEnv).resolve(Constants.INTERPOLATION_LIST))) {
				var listFile = super.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", Constants.INTERPOLATION_LIST);
				try (var input = listFile.openInputStream()) {
					Arrays.stream(new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
						.map(l -> new MethodInfo(l))
						.forEach(i -> allInterpolations.put(i.intepolationClass, i));
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

		var data = String.join(Constants.NEW_LINE, (allInterpolations.values().stream().map(i -> i.pack()).toList()));

		try {
			try (var output = new BufferedOutputStream(
				super.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Constants.INTERPOLATION_LIST).openOutputStream())) {
				output.write(data.getBytes(Constants.CHARSET));
			}
		} catch (IOException e) {
			super.processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
			return false;
		}

		return true;
	}

	private void execute(Element e) {
		var executable = ProcessorUtils.toExecutableElement(e);

		var returnType = executable.getReturnType();

		var typeElement = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(executable.getReturnType()));

		if (!ProcessorUtils.sameClass(typeElement, HalfAtom.class)) {
			//メソッドeは、返す型としてHalfAtomを必要とします
			error(
				"Method ["
					+ e.getSimpleName()
					+ "] requires returning "
					+ HalfAtom.class.getSimpleName(),
				e);

			return;
		}

		String generatePackageName;
		String generateClassName;
		{
			var args = ProcessorUtils.getTypeArgument(returnType);

			if (args.size() != 2) {
				error(HalfAtom.class.getSimpleName() + " requires two type arguments", e);
				return;
			}

			var typeArg = args.get(1);

			var element = ProcessorUtils.toElement(typeArg);
			if (element == null) {
				//AtomInterpolator<DataObject, ?>とされた場合
				error(HalfAtom.class.getSimpleName() + " requires two type arguments", e);
				return;
			}

			var clazz = ProcessorUtils.toTypeElement(element);
			var className = clazz.getQualifiedName().toString();

			generatePackageName = ProcessorUtils.getPackageName(clazz);

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

		var info = allInterpolations.get(newClassName);
		if (info != null && (!info.clazz.equals(className) || !info.method.equals(methodName))) {
			//generateClassNameという名前は既に他で使われています
			error("The name [" + generateClassName + "] has already been used elsewhere", e);
			return;
		}

		allInterpolations.put(newClassName, new MethodInfo(newClassName, className, methodName));

		if (alreadyCreatedFiles.contains(newClassName)) return;

		var sql = result.sql;

		var template = Formatter.readTemplate(SqlInterpolationTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("PROCESSOR", SqlInterpolationAnnotationProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("CLASS", generateClassName);

		var dubplicateChecker = new HashSet<String>();
		var fields = new LinkedList<String>();
		InterpolationPlaceholderFinder.execute(sql, variable -> {
			//重複は除外
			if (dubplicateChecker.contains(variable)) return;

			dubplicateChecker.add(variable);

			var method = "public "
				+ Atom.class.getName()
				+ "<?> "
				+ variable
				+ ";";
			fields.add(method);
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

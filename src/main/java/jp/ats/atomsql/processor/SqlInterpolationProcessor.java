package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.AtomSqlUtils;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.HalfAtom;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

/**
 * @author 千葉 哲嗣
 */
class SqlInterpolationProcessor {

	private final Supplier<ProcessingEnvironment> processingEnv;

	private final MethodExtractor extractor;

	//生成クラス名, メソッド名
	private final Map<String, MethodInfo> allInterpolations = new HashMap<>();

	SqlInterpolationProcessor(Supplier<ProcessingEnvironment> processingEnv) {
		this.processingEnv = processingEnv;
		extractor = new MethodExtractor(processingEnv);
	}

	void process(TypeElement annotation, RoundEnvironment roundEnv) {
		var env = processingEnv.get();
		try {
			//他のクラスで作られた過去分を追加
			if (Files.exists(ProcessorUtils.getClassOutputPath(env).resolve(Constants.INTERPOLATION_LIST))) {
				var listFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", Constants.INTERPOLATION_LIST);
				try (var input = listFile.openInputStream()) {
					Arrays.stream(new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
						.filter(l -> l.length() > 0)//空の場合スキップ
						.map(l -> new MethodInfo(l))
						.forEach(i -> allInterpolations.put(i.generatedClass, i));
				}
			}
		} catch (IOException e) {
			env.getMessager().printMessage(Kind.ERROR, e.getMessage());
			return;
		}

		roundEnv.getElementsAnnotatedWith(annotation).forEach(e -> {
			try {
				execute(e);
			} catch (ProcessException pe) {
				//スキップして次の対象へ
			}
		});

		var data = String.join(Constants.NEW_LINE, (allInterpolations.values().stream().map(i -> i.pack()).toList()));

		try (var output = new BufferedOutputStream(
			env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Constants.INTERPOLATION_LIST).openOutputStream())) {
			output.write(data.getBytes(Constants.CHARSET));
		} catch (IOException e) {
			env.getMessager().printMessage(Kind.ERROR, e.getMessage());
		}
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
		if (info != null && (!info.enclosingClass.equals(className) || !info.method.equals(methodName))) {
			//generateClassNameという名前は既に他で使われています
			error("The name [" + generateClassName + "] has already been used elsewhere", e);
			return;
		}

		allInterpolations.put(newClassName, new MethodInfo(newClassName, className, methodName));

		var sql = result.sql;

		var template = Formatter.readTemplate(SqlInterpolationTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("PROCESSOR", SqlInterpolationProcessor.class.getName());

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
			try (var output = new BufferedOutputStream(processingEnv.get().getFiler().createSourceFile(newClassName, e).openOutputStream())) {
				output.write(template.getBytes(Constants.CHARSET));
			}
		} catch (IOException ioe) {
			error(ioe.getMessage(), e);
		}
	}

	private void error(String message, Element e) {
		processingEnv.get().getMessager().printMessage(Kind.ERROR, message, e);
	}
}

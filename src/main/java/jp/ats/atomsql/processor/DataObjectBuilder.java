package jp.ats.atomsql.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.ColumnFinder;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.Protoatom;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.type.OBJECT;

/**
 * @author 千葉 哲嗣
 */
class DataObjectBuilder extends SourceBuilder {

	private final AtomSqlTypeFactory typeFactory;

	DataObjectBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
		typeFactory = AtomSqlTypeFactory.newInstanceForProcessor(AtomSql.configure().typeFactoryClass());
	}

	@Override
	ExtractResult extractTargetElement(ExecutableElement method) {
		var returnType = method.getReturnType();

		var typeElement = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(returnType));

		var requires = List.of(List.class, Stream.class, Optional.class, Atom.class, Protoatom.class);

		var found = requires.stream().filter(c -> ProcessorUtils.sameClass(typeElement, c)).findFirst();

		if (found.isEmpty()) {
			//メソッドeは、返す型としてrequiresを必要とします
			error(
				"Method ["
					+ method.getSimpleName()
					+ "] requires returning "
					+ String.join(" or ", requires.stream().map(c -> c.getSimpleName()).toList()),
				method);

			return ExtractResult.fail;
		}

		var args = ProcessorUtils.getTypeArgument(returnType);

		if (args.isEmpty()) {
			error(Protoatom.class.getSimpleName() + " requires type argument(s)", method);

			return ExtractResult.fail;
		}

		var element = ProcessorUtils.toElement(args.get(0));
		if (element == null) {
			//?とされた場合
			error(found.get().getSimpleName() + " requires " + DataObject.class.getSimpleName() + " type arguments", method);

			return ExtractResult.fail;
		}

		return new ExtractResult(true, element);
	}

	private List<String> columns(ExecutableElement method, String sql) {
		var dubplicateChecker = new HashSet<String>();
		var columns = new LinkedList<String>();

		ColumnFinder.execute(sql, f -> {
			var column = f.column;
			//重複は除外
			if (dubplicateChecker.contains(column)) return;

			dubplicateChecker.add(column);

			var type = f.typeHint.map(typeFactory::typeOf).orElse(OBJECT.instance).typeExpression();

			columns.add(type + " " + column);
		});

		return columns;
	}

	@Override
	String source(String generateClassName, ExecutableElement method, Result result) {
		var template = Formatter.readTemplate(DataObject_Template.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		param.put("COLUMNS", String.join("," + Constants.NEW_LINE, columns(method, result.sql)));

		return Formatter.format(template, param);
	}
}

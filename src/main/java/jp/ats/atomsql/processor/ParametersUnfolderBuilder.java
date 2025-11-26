package jp.ats.atomsql.processor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.PlaceholderFinder;
import jp.ats.atomsql.type.OBJECT;

class ParametersUnfolderBuilder extends UnfolderBuilder {

	private final AtomSqlTypeFactory typeFactory;

	ParametersUnfolderBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
		typeFactory = AtomSqlTypeFactory.newInstanceForProcessor(AtomSql.configure().typeFactoryClass());
	}

	@Override
	ExtractResult extractTargetElement(ExecutableElement method) {
		var parameters = method.getParameters();

		if (parameters.size() != 1 || !ProcessorUtils.sameClass(toTypeElement(parameters.get(0)), Consumer.class)) {
			//メソッドeは、Consumerの1つのパラメータを必要とします
			error(
				"Method ["
					+ method.getSimpleName()
					+ "] requires one parameter of Consumer",
				method);

			return ExtractResult.fail;
		}

		var p = parameters.get(0);

		var typeArgs = ProcessorUtils.getTypeArgument(p);
		if (typeArgs.size() == 0) {
			//Consumerと書かれた場合
			//Consumerは、型の引数を必要とします
			error("Consumer requires a type argument", p);

			return ExtractResult.fail;
		}

		var typeArg = typeArgs.get(0);

		var element = ProcessorUtils.toElement(typeArg);
		if (element == null) {
			//Consumer<?>と書かれた場合
			//Consumerは、型の引数を必要とします
			error("Consumer requires a type argument", p);

			return ExtractResult.fail;
		}

		return new ExtractResult(true, element);
	}

	@Override
	List<String> fields(ExecutableElement method, String sql) {
		var dubplicateChecker = new HashSet<String>();
		var fields = new LinkedList<String>();
		PlaceholderFinder.execute(sql, f -> {
			//重複は除外
			if (dubplicateChecker.contains(f.placeholder)) return;

			dubplicateChecker.add(f.placeholder);

			var typeArgument = f.typeArgumentHint
				.map(typeFactory::typeArgumentOf)
				.map(t -> "<" + t.typeArgumentExpression() + ">")
				.orElse("");

			var field = "public "
				+ f.typeHint.map(typeFactory::typeOf).orElse(OBJECT.instance).typeExpression()
				+ typeArgument
				+ " "
				+ f.placeholder
				+ ";";
			fields.add(field);
		});

		return fields;
	}

	private static TypeElement toTypeElement(VariableElement e) {
		return ProcessorUtils.toTypeElement(ProcessorUtils.toElement(e.asType()));
	}
}

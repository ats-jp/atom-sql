package jp.ats.atomsql.processor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.Prototype;

class AtomsUnfolderBuilder extends UnfolderBuilder {

	AtomsUnfolderBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
	}

	@Override
	ExtractResult extractTargetElement(ExecutableElement method) {
		var returnType = method.getReturnType();

		var typeElement = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(returnType));

		if (!ProcessorUtils.sameClass(typeElement, Prototype.class)) {
			//メソッドeは、返す型としてHalfAtomを必要とします
			error(
				"Method ["
					+ method.getSimpleName()
					+ "] requires returning "
					+ Prototype.class.getSimpleName(),
				method);

			return ExtractResult.fail;
		}

		var args = ProcessorUtils.getTypeArgument(returnType);

		if (args.size() != 2) {
			error(Prototype.class.getSimpleName() + " requires two type arguments", method);

			return ExtractResult.fail;
		}

		var typeArg = args.get(1);

		var element = ProcessorUtils.toElement(typeArg);
		if (element == null) {
			//AtomInterpolator<DataObject, ?>とされた場合
			error(Prototype.class.getSimpleName() + " requires two type arguments", method);

			return ExtractResult.fail;
		}

		return new ExtractResult(true, element);
	}

	@Override
	List<String> fields(ExecutableElement method, String sql) {
		var dubplicateChecker = new HashSet<String>();
		var fields = new LinkedList<String>();
		AtomPlaceholderFinder.execute(sql, variable -> {
			//重複は除外
			if (dubplicateChecker.contains(variable)) return;

			dubplicateChecker.add(variable);

			var field = "public "
				+ Atom.class.getName()
				+ "<?> "
				+ variable
				+ ";";
			fields.add(field);
		});

		return fields;
	}

}

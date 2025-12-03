package jp.ats.atomsql.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;

import jp.ats.atomsql.Constants;
import jp.ats.atomsql.processor.MethodExtractor.Result;

/**
 * @author 千葉 哲嗣
 */
abstract class UnfolderBuilder extends SourceBuilder {

	UnfolderBuilder(Supplier<ProcessingEnvironment> processingEnv, DuplicateClassChecker checker) {
		super(processingEnv, checker);
	}

	abstract List<String> fields(ExecutableElement method, String sql);

	@Override
	String source(String generateClassName, ExecutableElement method, Result result) {
		var template = Formatter.readTemplate(Unfolder_Template.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("GENERATED", this.getClass().getName());

		param.put("PACKAGE", result.packageName.isEmpty() ? "" : ("package " + result.packageName + ";"));
		param.put("CLASS", generateClassName);

		param.put("FIELDS", String.join(Constants.NEW_LINE, fields(method, result.sql)));

		return Formatter.format(template, param);
	}
}

package jp.ats.atomsql.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jp.ats.atomsql.Constants;
import jp.ats.atomsql.Utils;
import jp.ats.atomsql.annotation.Sql;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;

class MethodExtractor {

	private static final Class<?> DEFAULT_SQL_FILE_RESOLVER_CLASS = SimpleMavenSqlFileResolver.class;

	private final Supplier<ProcessingEnvironment> envSupplier;

	private SqlFileResolver resolver;

	MethodExtractor(Supplier<ProcessingEnvironment> envSupplier) {
		this.envSupplier = envSupplier;
	}

	static class Result {

		String className;

		String packageName;

		String sql;

	}

	Result execute(Element method) throws SqlFileNotFoundException {
		var env = envSupplier.get();

		var packageNameAndBinaryClassName = ProcessorUtils.getPackageNameAndBinaryClassName(method, env);

		var packageName = packageNameAndBinaryClassName.packageName();
		var className = packageNameAndBinaryClassName.binaryClassName();

		String sql;

		var sqlAnnotation = method.getAnnotation(Sql.class);
		if (sqlAnnotation != null) {
			sql = sqlAnnotation.value();
		} else {
			//SQLファイルはクラスのバイナリ名と一致していないといけない
			var classBinaryName = Utils.extractSimpleClassName(className, packageName);
			var sqlFileName = classBinaryName + "." + method.getSimpleName() + ".sql";

			try {
				sql = new String(
					resolver(method).resolve(
						ProcessorUtils.getClassOutputPath(env),
						packageName.toString(),
						sqlFileName,
						env.getOptions()),
					Constants.CHARSET);
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}

		var result = new Result();
		result.sql = sql;
		result.className = className;
		result.packageName = packageName;

		return result;
	}

	private SqlFileResolver resolver(Element method) {
		if (resolver != null) return resolver;

		var className = envSupplier.get().getOptions().get("sql-file-resolver");

		var clazz = DEFAULT_SQL_FILE_RESOLVER_CLASS;
		if (className != null) {
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				//クラスclassNameは見つかりませんでした
				error("Class [" + className + "] was not found", method);
				throw new ProcessException();
			}
		}

		try {
			resolver = (SqlFileResolver) clazz.getConstructor().newInstance();
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
			| InstantiationException e) {
			//クラスclassNameのインスタンス化の際にエラーが発生しました
			error("An error occurred when instantiating class [" + className + "]", method);
			throw new ProcessException();
		}

		return resolver;
	}

	private void error(String message, Element e) {
		envSupplier.get().getMessager().printMessage(Kind.ERROR, message, e);
	}
}

package jp.ats.atomsql.processor;

import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlProxy;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes({
	"jp.ats.atomsql.annotation.SqlProxy",
	"jp.ats.atomsql.annotation.DataObject",
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SuppressWarnings("javadoc")
public class AtomSqlProcessor extends AbstractProcessor {

	static {
		AtomSql.initializeIfUninitialized();
	}

	private final SqlProxyProcessor sqlProxyAnnotationProcessor;

	private final DataObjectProcessor dataObjectAnnotationProcessor;

	public AtomSqlProcessor() {
		Supplier<ProcessingEnvironment> supplier = () -> processingEnv;
		sqlProxyAnnotationProcessor = new SqlProxyProcessor(supplier);
		dataObjectAnnotationProcessor = new DataObjectProcessor(supplier);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		annotations.forEach(a -> {
			if (ProcessorUtils.sameClass(a, SqlProxy.class)) {
				sqlProxyAnnotationProcessor.process(a, roundEnv);
			} else if (ProcessorUtils.sameClass(a, DataObject.class)) {
				dataObjectAnnotationProcessor.process(a, roundEnv);
			}
		});

		return true;
	}
}

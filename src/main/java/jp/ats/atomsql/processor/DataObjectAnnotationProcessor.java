package jp.ats.atomsql.processor;

import java.sql.ResultSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic.Kind;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.processor.MetadataBuilder.MethodInfo;
import jp.ats.atomsql.processor.MetadataBuilder.MethodVisitor;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes("jp.ats.atomsql.annotation.DataObject")
@SupportedSourceVersion(SourceVersion.RELEASE_16)
public class DataObjectAnnotationProcessor extends AbstractProcessor {

	private final ResultTypeChecker resultTypeChecker = new ResultTypeChecker();

	private final TypeNameExtractor typeNameExtractor = new TypeNameExtractor(() -> DataObjectAnnotationProcessor.super.processingEnv);

	private final DataObjectAnnotationProcessorMethodVisitor methodVisitor = new DataObjectAnnotationProcessorMethodVisitor();

	private final MetadataBuilder builder = new MetadataBuilder(() -> super.processingEnv, methodVisitor);

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		annotations.forEach(a -> {
			roundEnv.getElementsAnnotatedWith(a).forEach(e -> {
				ElementKind kind = e.getKind();
				if (kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
					//kindにDataObjectを注釈することはできません
					error("Cannot annotate " + kind.name() + " with " + DataObject.class.getSimpleName(), e);

					return;
				}

				var visitor = new Visitor();
				e.getEnclosedElements().forEach(enc -> {
					enc.accept(visitor, null);
				});

				//レコードの場合、コンストラクタの引数名称を保存
				if (kind == ElementKind.RECORD) {
					builder.build(e);
					return;
				}
			});
		});

		return false;
	}

	private class Visitor extends SimpleElementVisitor14<Boolean, Void> {

		@Override
		public Boolean visitExecutable(ExecutableElement e, Void p) {
			//コンストラクタ以外はスキップ
			if (!"<init>".equals(e.getSimpleName().toString()))
				return true;

			//レコードのコンストラクタの場合はスキップ
			if (e.getEnclosingElement().getKind() == ElementKind.RECORD)
				return true;

			var params = e.getParameters();

			//パラメータなしコンストラクタはOK
			if (params.size() == 0) {
				return true;
			}

			//DataObjectは、パラメーターがResultSetのみであるか、パラメーターがないコンストラクターが必要です
			var errorMessage = DataObject.class.getSimpleName() + " requires a constructor with only ResultSet parameter or no parameter";

			if (params.size() != 1) {
				error(errorMessage, e);
				return false;
			}

			//パラメータがResultSetのみのコンストラクタはOK
			if (!params.get(0).asType().accept(ParameterTypeChecker.instance, null)) {
				error(errorMessage, e);
				return false;
			}

			return true;
		}

		@Override
		public Boolean visitRecordComponent(RecordComponentElement e, Void p) {
			return e.asType().accept(resultTypeChecker, e);
		}

		@Override
		public Boolean visitVariable(VariableElement e, Void p) {
			return e.asType().accept(resultTypeChecker, e);
		}
	}

	private class ResultTypeChecker extends SimpleTypeVisitor14<Boolean, Element> {

		@Override
		protected Boolean defaultAction(TypeMirror e, Element p) {
			//結果タイプeは使用できません
			error("Result type [" + e + "] cannot be used", p);
			return false;
		}

		@Override
		public Boolean visitPrimitive(PrimitiveType t, Element p) {
			return switch (t.getKind()) {
			case BOOLEAN, BYTE, DOUBLE, FLOAT, INT, LONG -> true;
			default -> defaultAction(t, p);
			};
		}

		@Override
		public Boolean visitDeclared(DeclaredType t, Element p) {
			TypeElement type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, AtomSqlType.BIG_DECIMAL.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.BINARY_STREAM.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.BLOB.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.BOOLEAN.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.BYTE_ARRAY.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.CHARACTER_STREAM.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.CLOB.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.DOUBLE.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.FLOAT.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.INTEGER.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.LONG.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.STRING.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.DATE.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.TIME.type()))
				return true;
			if (ProcessorUtils.sameClass(type, AtomSqlType.DATETIME.type()))
				return true;

			return defaultAction(t, p);
		}
	}

	private static class ParameterTypeChecker extends SimpleTypeVisitor14<Boolean, Void> {

		private static final ParameterTypeChecker instance = new ParameterTypeChecker();

		@Override
		protected Boolean defaultAction(TypeMirror e, Void p) {
			return false;
		}

		@Override
		public Boolean visitDeclared(DeclaredType t, Void p) {
			TypeElement type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, ResultSet.class)) {
				return true;
			}

			return defaultAction(t, p);
		}
	}

	private class DataObjectAnnotationProcessorMethodVisitor extends MethodVisitor {

		@Override
		public Void visitExecutable(ExecutableElement e, List<MethodInfo> p) {
			if (e.getKind() != ElementKind.CONSTRUCTOR) return DEFAULT_VALUE;

			//使用できる型のチェックは前段階でレコードコンポーネントを対象に行っているので、ここではチェックを行わない

			var info = new MethodInfo();

			info.name = e.getSimpleName().toString();

			var parameters = e.getParameters();
			parameters.forEach(parameter -> {
				info.parameterNames.add(parameter.getSimpleName().toString());
				info.parameterTypes.add(parameter.asType().accept(typeNameExtractor, e));
			});

			p.add(info);

			return DEFAULT_VALUE;
		}
	}

	private void error(String message, Element e) {
		super.processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}
}

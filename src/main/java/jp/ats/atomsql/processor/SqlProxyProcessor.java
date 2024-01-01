package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.AtomSql;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.AtomSqlUtils;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.HalfAtom;
import jp.ats.atomsql.PlaceholderFinder;
import jp.ats.atomsql.annotation.AtomSqlSupplier;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlInterpolation;
import jp.ats.atomsql.annotation.SqlParameters;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.annotation.SqlProxySupplier;
import jp.ats.atomsql.processor.MetadataBuilder.MethodInfo;
import jp.ats.atomsql.processor.MetadataBuilder.MethodVisitor;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.MethodExtractor.SqlNotFoundException;
import jp.ats.atomsql.processor.SqlFileResolver.SqlFileNotFoundException;
import jp.ats.atomsql.type.CSV;

/**
 * @author 千葉 哲嗣
 */
class SqlProxyProcessor {

	private final Supplier<ProcessingEnvironment> processingEnv;

	private final AtomSqlTypeFactory typeFactory;

	private final TypeNameExtractor typeNameExtractor;

	private final SqlProxyAnnotationProcessorMethodVisitor methodVisitor = new SqlProxyAnnotationProcessorMethodVisitor();

	private final ReturnTypeChecker returnTypeChecker = new ReturnTypeChecker();

	private final Set<String> sqlProxyList = new HashSet<>();

	private final MethodExtractor extractor;

	private final MetadataBuilder builder;

	public SqlProxyProcessor(Supplier<ProcessingEnvironment> processingEnv) {
		this.processingEnv = processingEnv;
		typeFactory = AtomSqlTypeFactory.newInstanceForProcessor(AtomSql.configure().typeFactoryClass());
		typeNameExtractor = new TypeNameExtractor(processingEnv);
		extractor = new MethodExtractor(processingEnv);
		builder = new MetadataBuilder(processingEnv, methodVisitor);
	}

	void process(TypeElement annotation, RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(annotation).forEach(e -> {
			try {
				execute(e);
			} catch (ProcessException pe) {
				//スキップして次の対象へ
				//ProcessExceptionはスキップするための例外
			}
		});

		var env = processingEnv.get();
		try {
			//他のプロセスで作られた過去分を追加
			if (Files.exists(ProcessorUtils.getClassOutputPath(env).resolve(Constants.PROXY_LIST))) {
				var listFile = env.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", Constants.PROXY_LIST);
				try (var input = listFile.openInputStream()) {
					Arrays.stream(
						new String(AtomSqlUtils.readBytes(input), Constants.CHARSET).split("\\s+"))
						.forEach(l -> sqlProxyList.add(l));
				}
			}

			var data = String.join(Constants.NEW_LINE, sqlProxyList).getBytes(Constants.CHARSET);

			try (var output = new BufferedOutputStream(
				env.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", Constants.PROXY_LIST).openOutputStream())) {
				output.write(data);
			}
		} catch (IOException ioe) {
			env.getMessager().printMessage(Kind.ERROR, ioe.getMessage());
		}

		sqlProxyList.clear();
	}

	private void execute(Element e) {
		var kind = e.getKind();
		if (kind != ElementKind.INTERFACE) {
			//kindにSqlProxyを注釈することはできません
			error("Cannot annotate " + kind.name() + " with " + SqlProxy.class.getSimpleName(), e);

			throw new ProcessException();
		}

		builder.build(e);

		if (!builder.hasError())
			sqlProxyList.add(processingEnv.get().getElementUtils().getBinaryName(ProcessorUtils.toTypeElement(e)).toString());
	}

	private void error(String message, Element e) {
		processingEnv.get().getMessager().printMessage(Kind.ERROR, message, e);
	}

	private static record ReturnTypeCheckerResult(TypeMirror dataType, TypeMirror sqlInterpolationType) {

		private static final ReturnTypeCheckerResult defaultValue = new ReturnTypeCheckerResult(null, null);
	}

	private class ReturnTypeChecker extends SimpleTypeVisitor14<ReturnTypeCheckerResult, ExecutableElement> {

		private ReturnTypeCheckerResult errorAction(TypeMirror t, ExecutableElement p) {
			//リターンタイプtは使用できません
			error("Return type [" + t + "] cannot be used", p);
			builder.setError();
			return ReturnTypeCheckerResult.defaultValue;
		}

		@Override
		protected ReturnTypeCheckerResult defaultAction(TypeMirror t, ExecutableElement p) {
			if (t.getKind() == TypeKind.VOID) return ReturnTypeCheckerResult.defaultValue;

			return errorAction(t, p);
		}

		@Override
		public ReturnTypeCheckerResult visitPrimitive(PrimitiveType t, ExecutableElement p) {
			return switch (t.getKind()) {
			case INT -> ReturnTypeCheckerResult.defaultValue;
			default -> errorAction(t, p);
			};
		}

		@Override
		public ReturnTypeCheckerResult visitDeclared(DeclaredType t, ExecutableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, HalfAtom.class)) {
				return processHalfAtomType(t, p);
			}

			if (ProcessorUtils.sameClass(type, List.class) || ProcessorUtils.sameClass(type, Optional.class) || ProcessorUtils.sameClass(type, Stream.class) || ProcessorUtils.sameClass(type, Atom.class)) {
				var dataType = t.getTypeArguments().get(0);

				if (dataType.accept(AnnotationExtractor.instance, null)) {
					return new ReturnTypeCheckerResult(dataType, null);
				}

				if (ProcessorUtils.toElement(dataType) == null) {
					if (ProcessorUtils.sameClass(type, Atom.class)) {
						// <?>
						// Atomの場合は、型パラメータを指定しなくてOK
						return ReturnTypeCheckerResult.defaultValue;
					}

					// <?>
					// Stream, List, Optionalの場合は、型パラメータを指定しなければならない
					return errorAction(t, p);
				}

				if (typeFactory.canUseForProcessor(ProcessorUtils.toTypeElement(dataType))) {
					return new ReturnTypeCheckerResult(dataType, null);
				}

				return errorDataType(dataType, p);
			}

			return errorAction(t, p);
		}

		private ReturnTypeCheckerResult errorDataType(TypeMirror e, ExecutableElement p) {
			//データオブジェクトクラスeはAtom SQLで検索結果として使用可能なクラスか、@DataObjectで注釈されていなければなりません
			error("Data class [" + e + "] must be available as search result in Atom SQL or annotated @" + DataObject.class.getSimpleName(), p);
			builder.setError();
			return ReturnTypeCheckerResult.defaultValue;
		}

		//自動生成クラスがまだ作成されていない場合
		@Override
		public ReturnTypeCheckerResult visitError(ErrorType t, ExecutableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, HalfAtom.class)) {
				return processHalfAtomType(t, p);
			}

			return errorAction(t, p);
		}

		private ReturnTypeCheckerResult processHalfAtomType(DeclaredType t, ExecutableElement p) {
			var annotation = p.getAnnotation(SqlInterpolation.class);
			if (annotation == null) {
				error("Annotation @" + SqlInterpolation.class.getSimpleName() + " required", p);
				builder.setError();
				return ReturnTypeCheckerResult.defaultValue;
			}

			var dataType = t.getTypeArguments().get(0);

			if (ProcessorUtils.toElement(dataType) == null) {
				// <?>
				// HalfAtomの場合は、結果型パラメータを指定しなくてOK
				dataType = null;
			} else if (!dataType.accept(AnnotationExtractor.instance, null) && !typeFactory.canUseForProcessor(ProcessorUtils.toTypeElement(dataType))) {
				return errorDataType(dataType, p);
			}

			var interpolationType = t.getTypeArguments().get(1);
			if (ProcessorUtils.toElement(interpolationType) == null) {
				return errorAction(t, p);
			}

			return new ReturnTypeCheckerResult(dataType, interpolationType);
		}
	}

	private class ParameterTypeChecker extends SimpleTypeVisitor14<TypeMirror, VariableElement> {

		private final ExecutableElement method;

		private ParameterTypeChecker(ExecutableElement method) {
			this.method = method;
		}

		@Override
		protected TypeMirror defaultAction(TypeMirror e, VariableElement p) {
			//パラメータタイプeは使用できません
			error("Parameter type [" + e + "] cannot be used", p);
			builder.setError();
			return DEFAULT_VALUE;
		}

		@Override
		public TypeMirror visitPrimitive(PrimitiveType t, VariableElement p) {
			return switch (t.getKind()) {
			case BOOLEAN, BYTE, DOUBLE, FLOAT, INT, LONG -> DEFAULT_VALUE;
			default -> defaultAction(t, p);
			};
		}

		@Override
		public TypeMirror visitArray(ArrayType t, VariableElement p) {
			if (t.getComponentType().getKind() == TypeKind.BYTE) return DEFAULT_VALUE;

			return defaultAction(t, p);
		}

		@Override
		public TypeMirror visitDeclared(DeclaredType t, VariableElement p) {
			TypeElement type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, Consumer.class)) {
				return processConsumerType(p);
			}

			if (typeFactory.canUseForProcessor(type)) return DEFAULT_VALUE;

			var csvType = new CSV(typeFactory).type();
			if (ProcessorUtils.sameClass(type, csvType)) {
				var argumentType = t.getTypeArguments().get(0);
				var element = ProcessorUtils.toElement(argumentType);
				var typeElement = ProcessorUtils.toTypeElement(element);

				// この先再帰するので同タイプは先にはじく
				if (ProcessorUtils.sameClass(typeElement, csvType)) {
					return defaultAction(t, p);
				}

				var checker = new ParameterTypeChecker(method);
				argumentType.accept(checker, p);

				return DEFAULT_VALUE;
			}

			return defaultAction(t, p);
		}

		//自動生成クラスがまだ作成されていない場合
		@Override
		public TypeMirror visitError(ErrorType t, VariableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, Consumer.class)) {
				return processConsumerType(p);
			}

			return defaultAction(t, p);
		}

		private TypeMirror processConsumerType(VariableElement p) {
			var annotation = method.getAnnotation(SqlParameters.class);
			if (annotation == null) {
				error("Annotation @" + SqlParameters.class.getSimpleName() + " required", p);
				builder.setError();
				return DEFAULT_VALUE;
			}

			var args = ProcessorUtils.getTypeArgument(p);
			if (args.size() == 0) {
				error("Consumer requires a type argument", p);
				builder.setError();
				return DEFAULT_VALUE;
			}

			return args.get(0);
		}
	}

	private static TypeElement returnTypeOf(ExecutableElement e) {
		return ProcessorUtils.toTypeElement(ProcessorUtils.toElement(e.getReturnType()));
	}

	private class SqlProxyAnnotationProcessorMethodVisitor extends MethodVisitor {

		@Override
		public Void visitExecutable(ExecutableElement e, List<MethodInfo> p) {
			var modifiers = e.getModifiers();
			if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC)) {
				//デフォルトメソッド、スタティックメソッドは対象外
				return DEFAULT_VALUE;
			}

			if (e.getAnnotation(AtomSqlSupplier.class) != null) {
				if (!ProcessorUtils.sameClass(returnTypeOf(e), AtomSql.class)) {
					error(
						"Annotation "
							+ AtomSqlSupplier.class.getSimpleName()
							+ " requires returning "
							+ AtomSql.class.getSimpleName(),
						e);

					builder.setError();
				}

				if (e.getParameters().size() != 0) {
					error(
						"Annotation "
							+ AtomSqlSupplier.class.getSimpleName()
							+ " requires 0 parameters",
						e);

					builder.setError();
				}

				return DEFAULT_VALUE;
			}

			var info = new MethodInfo();

			info.name = e.getSimpleName().toString();

			if (e.getAnnotation(SqlProxySupplier.class) != null) {
				var returnType = returnTypeOf(e);

				if (returnType.getAnnotation(SqlProxy.class) == null) {
					error(
						"Annotation "
							+ SqlProxySupplier.class.getSimpleName()
							+ " requires returning "
							+ SqlProxy.class.getSimpleName()
							+ " annotated class",
						e);

					builder.setError();

					return DEFAULT_VALUE;
				}

				if (e.getParameters().size() != 0) {
					error(
						"Annotation "
							+ SqlProxySupplier.class.getSimpleName()
							+ " requires 0 parameters",
						e);

					builder.setError();

					return DEFAULT_VALUE;
				}

				info.sqlProxy = returnType.getQualifiedName().toString();
				p.add(info);

				return DEFAULT_VALUE;
			}

			var parameters = e.getParameters();

			var checker = new ParameterTypeChecker(e);

			parameters.forEach(parameter -> {
				info.parameterNames.add(parameter.getSimpleName().toString());

				info.parameterTypes.add(parameter.asType().accept(typeNameExtractor, e));

				var typeArg = parameter.asType().accept(checker, parameter);
				if (typeArg != null) {
					info.sqlParameters = typeArg.accept(typeNameExtractor, e);
				}
			});

			//チェッカーでチェックした中でエラーがあった場合
			if (builder.hasError()) return DEFAULT_VALUE;

			Result result;
			try {
				//SQLファイルが存在するかチェックするために実行
				result = extractor.execute(e);
			} catch (SqlNotFoundException | SqlFileNotFoundException ex) {
				error(ex.getMessage(), e);

				builder.setError();

				return DEFAULT_VALUE;
			}

			if (info.sqlParameters == null) {
				//通常のメソッド引数が存在するので検査対象
				Set<String> placeholders = new TreeSet<>();
				PlaceholderFinder.execute(result.sql, f -> {
					placeholders.add(f.placeholder);
				});

				if (!new TreeSet<>(info.parameterNames).equals(placeholders)) {
					//SQLのプレースホルダーがパラメータの名前と一致しません
					error("SQL placeholders do not match parameter names", e);

					builder.setError();

					return DEFAULT_VALUE;
				}
			}

			var returnTypeCheckerResult = e.getReturnType().accept(returnTypeChecker, e);

			if (returnTypeCheckerResult.dataType != null) {
				info.dataType = returnTypeCheckerResult.dataType.accept(typeNameExtractor, e);
			}

			if (returnTypeCheckerResult.sqlInterpolationType != null) {
				info.sqlInterpolation = returnTypeCheckerResult.sqlInterpolationType.accept(typeNameExtractor, e);
			}

			p.add(info);

			return DEFAULT_VALUE;
		}
	}

	private static class AnnotationExtractor extends SimpleTypeVisitor14<Boolean, ExecutableElement> {

		private static final AnnotationExtractor instance = new AnnotationExtractor();

		@Override
		protected Boolean defaultAction(TypeMirror e, ExecutableElement p) {
			return false;
		}

		@Override
		public Boolean visitDeclared(DeclaredType t, ExecutableElement p) {
			return t.asElement().getAnnotation(DataObject.class) != null;
		}
	}
}

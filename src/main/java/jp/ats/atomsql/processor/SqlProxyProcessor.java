package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.processing.Generated;
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
import jp.ats.atomsql.ColumnFinder;
import jp.ats.atomsql.ColumnFinder.Found;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.PlaceholderFinder;
import jp.ats.atomsql.Protoatom;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlProxy;
import jp.ats.atomsql.processor.MetadataBuilder.MethodInfo;
import jp.ats.atomsql.processor.MetadataBuilder.MethodVisitor;
import jp.ats.atomsql.processor.MethodExtractor.Result;
import jp.ats.atomsql.processor.MethodExtractor.SqlNotFoundException;
import jp.ats.atomsql.processor.SourceBuilder.DuplicateClassChecker;
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

	private final Set<String> sqlProxyList = new HashSet<>();

	private final MethodExtractor methodExtractor;

	private final MetadataBuilder metadataBuilder;

	private final DuplicateClassChecker duplicateClassChecker;

	private final ParametersUnfolderBuilder parametersUnfolderBuilder;

	private final ProtoatomUnfolderBuilder protoatomUnfolderBuilder;

	private final DataObjectBuilder dataObjectBuilder;

	public SqlProxyProcessor(Supplier<ProcessingEnvironment> processingEnv) {
		this.processingEnv = processingEnv;
		typeFactory = AtomSqlTypeFactory.newInstanceForProcessor(AtomSql.configure().typeFactoryClass());
		typeNameExtractor = new TypeNameExtractor(processingEnv);
		methodExtractor = new MethodExtractor(processingEnv);
		metadataBuilder = new MetadataBuilder(processingEnv, methodVisitor);

		duplicateClassChecker = new DuplicateClassChecker();
		parametersUnfolderBuilder = new ParametersUnfolderBuilder(processingEnv, duplicateClassChecker);
		protoatomUnfolderBuilder = new ProtoatomUnfolderBuilder(processingEnv, duplicateClassChecker);
		dataObjectBuilder = new DataObjectBuilder(processingEnv, duplicateClassChecker);
	}

	void process(TypeElement annotation, RoundEnvironment roundEnv) {
		var env = processingEnv.get();

		try {
			duplicateClassChecker.start(env);

			roundEnv.getElementsAnnotatedWith(annotation).forEach(e -> {
				try {
					execute(e);
				} catch (ProcessException pe) {
					//スキップして次の対象へ
					//ProcessExceptionはスキップするための例外
				}
			});

		} finally {
			duplicateClassChecker.finish(env);
		}

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

		metadataBuilder.build(e);

		if (!metadataBuilder.hasError())
			sqlProxyList.add(processingEnv.get().getElementUtils().getBinaryName(ProcessorUtils.toTypeElement(e)).toString());
	}

	private void error(String message, Element e) {
		processingEnv.get().getMessager().printMessage(Kind.ERROR, message, e);
	}

	private static record ReturnTypeCheckerResult(boolean retry, TypeMirror dataType, TypeMirror protoatomUnfolderType) {

		private static final ReturnTypeCheckerResult defaultValue = new ReturnTypeCheckerResult(false, null, null);

		private static final ReturnTypeCheckerResult retryNotice = new ReturnTypeCheckerResult(true, null, null);

		private static ReturnTypeCheckerResult newInstance(TypeMirror dataType, TypeMirror protoatomUnfolderType) {
			return new ReturnTypeCheckerResult(false, dataType, protoatomUnfolderType);
		}
	}

	private class ReturnTypeChecker extends SimpleTypeVisitor14<ReturnTypeCheckerResult, ExecutableElement> {

		private final boolean needsDataObjectBuild;

		private ReturnTypeChecker(boolean needsDataObjectBuild) {
			this.needsDataObjectBuild = needsDataObjectBuild;
		}

		private ReturnTypeCheckerResult errorAction(TypeMirror t, ExecutableElement p) {
			//リターンタイプtは使用できません
			error("Return type [" + t + "] cannot be used", p);
			metadataBuilder.setError();
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

		private boolean isGeneratedDataObject(TypeMirror t) {
			return typeElement(t).map(e -> {
				var annotation = e.getAnnotation(Generated.class);

				if (annotation == null) return false;

				var value = annotation.value();

				if (value == null || value.length != 1) return false;

				return DataObjectBuilder.class.getName().equals(value[0]);
			}).orElse(false);
		}

		@Override
		public ReturnTypeCheckerResult visitDeclared(DeclaredType t, ExecutableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, List.class)
				|| ProcessorUtils.sameClass(type, Optional.class)
				|| ProcessorUtils.sameClass(type, Stream.class)) {
				var dataType = t.getTypeArguments().get(0);

				if (needsDataObjectBuild && isGeneratedDataObject(dataType)) {
					dataObjectBuilder.execute(p);
				}

				if (isAnnotated(dataType, DataObject.class)) {
					return ReturnTypeCheckerResult.newInstance(dataType, null);
				}

				if (ProcessorUtils.toElement(dataType) == null) {
					// <?>
					// Stream, List, Optionalの場合は、型パラメータを指定しなければならない
					return errorAction(t, p);
				}

				if (ProcessorUtils.canUse(ProcessorUtils.toTypeElement(dataType), typeFactory)) {
					return ReturnTypeCheckerResult.newInstance(dataType, null);
				}

				return errorDataType(dataType, p);
			}

			if (ProcessorUtils.sameClass(type, Atom.class)) {
				var dataType = t.getTypeArguments().get(0);

				if (needsDataObjectBuild && isGeneratedDataObject(dataType)) {
					dataObjectBuilder.execute(p);
				}

				if (isAnnotated(dataType, DataObject.class)) {
					return ReturnTypeCheckerResult.newInstance(dataType, null);
				}

				if (ProcessorUtils.toElement(dataType) == null) {
					// <?>
					// Atomの場合は、型パラメータを指定しなくてOK
					return ReturnTypeCheckerResult.defaultValue;
				}

				var typeElement = ProcessorUtils.toTypeElement(dataType);
				// Atomの場合は、VoidでもOK
				//<?>が使用できないケースでの使用を想定
				//Atom<?>だと、Optional等で扱えないケースがあるため
				if (ProcessorUtils.sameClass(typeElement, Void.class)
					|| ProcessorUtils.canUse(typeElement, typeFactory)) {
					return ReturnTypeCheckerResult.newInstance(dataType, null);
				}

				return errorDataType(dataType, p);
			}

			if (ProcessorUtils.sameClass(type, Protoatom.class)) {
				if (needsDataObjectBuild && isGeneratedDataObject(t.getTypeArguments().get(0))) {
					dataObjectBuilder.execute(p);
				}

				protoatomUnfolderBuilder.execute(p);

				return processProtoatom(t, p);
			}

			return errorAction(t, p);
		}

		private ReturnTypeCheckerResult errorDataType(TypeMirror e, ExecutableElement p) {
			//データオブジェクトクラスeはAtom SQLで検索結果として使用可能なクラスか、@DataObjectで注釈されていなければなりません
			error("Data class [" + e + "] must be available as search result in Atom SQL or annotated @" + DataObject.class.getSimpleName(), p);
			metadataBuilder.setError();
			return ReturnTypeCheckerResult.defaultValue;
		}

		//自動生成クラスがまだ作成されていない場合
		@Override
		public ReturnTypeCheckerResult visitError(ErrorType t, ExecutableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (needsDataObjectBuild) {
				if (ProcessorUtils.sameClass(type, List.class)
					|| ProcessorUtils.sameClass(type, Optional.class)
					|| ProcessorUtils.sameClass(type, Stream.class)
					|| ProcessorUtils.sameClass(type, Atom.class)) {
					dataObjectBuilder.execute(p);

					return ReturnTypeCheckerResult.retryNotice;
				}
			}

			if (ProcessorUtils.sameClass(type, Protoatom.class)) {
				protoatomUnfolderBuilder.execute(p);

				if (needsDataObjectBuild) {
					dataObjectBuilder.execute(p);

					return ReturnTypeCheckerResult.retryNotice;
				}

				return processProtoatom(t, p);
			}

			return errorAction(t, p);
		}

		private ReturnTypeCheckerResult processProtoatom(DeclaredType t, ExecutableElement p) {
			var dataType = t.getTypeArguments().get(0);

			if (ProcessorUtils.toElement(dataType) == null) {
				// <?>
				// Protoatomの場合は、結果型パラメータを指定しなくてOK
				dataType = null;
			} else if (!isAnnotated(dataType, DataObject.class) && !ProcessorUtils.canUse(ProcessorUtils.toTypeElement(dataType), typeFactory)) {
				return errorDataType(dataType, p);
			}

			var protoatomUnfolderType = t.getTypeArguments().get(1);
			if (ProcessorUtils.toElement(protoatomUnfolderType) == null) {
				return errorAction(t, p);
			}

			return ReturnTypeCheckerResult.newInstance(dataType, protoatomUnfolderType);
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
			metadataBuilder.setError();
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
				parametersUnfolderBuilder.execute(method);
				return processConsumerType(p);
			}

			if (ProcessorUtils.canUse(type, typeFactory)) return DEFAULT_VALUE;

			var csvType = new CSV(typeFactory).type();
			if (ProcessorUtils.sameClass(type, csvType)) {
				var argumentType = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(t.getTypeArguments().get(0)));

				if (ProcessorUtils.canUse(argumentType, typeFactory)) return DEFAULT_VALUE;
			}

			return defaultAction(t, p);
		}

		//自動生成クラスがまだ作成されていない場合
		@Override
		public TypeMirror visitError(ErrorType t, VariableElement p) {
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, Consumer.class)) {
				parametersUnfolderBuilder.execute(method);
				return processConsumerType(p);
			}

			return defaultAction(t, p);
		}

		private TypeMirror processConsumerType(VariableElement p) {
			var args = ProcessorUtils.getTypeArgument(p);
			if (args.size() == 0) {
				error("Consumer requires a type argument", p);
				metadataBuilder.setError();
				return DEFAULT_VALUE;
			}

			return args.get(0);
		}
	}

	private static Optional<TypeElement> typeElement(TypeMirror type) {
		return Optional.ofNullable(ProcessorUtils.toElement(type)).map(ProcessorUtils::toTypeElement);
	}

	private static <A extends Annotation> boolean isAnnotated(TypeMirror type, Class<A> annotation) {
		return typeElement(type).map(e -> e.getAnnotation(annotation) != null).orElse(false);
	}

	private class SqlProxyAnnotationProcessorMethodVisitor extends MethodVisitor {

		@Override
		public Void visitExecutable(ExecutableElement e, List<MethodInfo> p) {
			var modifiers = e.getModifiers();
			if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC)) {
				//デフォルトメソッド、スタティックメソッドは対象外
				return DEFAULT_VALUE;
			}

			var info = new MethodInfo();

			info.name = e.getSimpleName().toString();

			var parameters = e.getParameters();

			var checker = new ParameterTypeChecker(e);

			parameters.forEach(parameter -> {
				info.parameterNames.add(parameter.getSimpleName().toString());

				info.parameterTypes.add(parameter.asType().accept(typeNameExtractor, e));

				var typeArg = parameter.asType().accept(checker, parameter);
				if (typeArg != null) {
					info.parametersUnfolder = typeArg.accept(typeNameExtractor, e);
				}
			});

			//チェッカーでチェックした中でエラーがあった場合
			if (metadataBuilder.hasError()) return DEFAULT_VALUE;

			var returnType = e.getReturnType();

			if (typeElement(returnType).map(ProcessorUtils::toTypeElement).map(returnTypeElement -> {
				if (ProcessorUtils.sameClass(returnTypeElement, AtomSql.class)) {
					if (e.getParameters().size() != 0) {
						error("Return type [" + AtomSql.class.getSimpleName() + "] requires 0 parameters", e);

						metadataBuilder.setError();
					}

					return true;
				}

				if (returnTypeElement.getAnnotation(SqlProxy.class) != null) {
					if (e.getParameters().size() != 0) {
						error("Return type annotated [" + SqlProxy.class.getSimpleName() + "] requires 0 parameters", e);

						metadataBuilder.setError();
					}

					return true;
				}

				return false;
			}).orElse(false)) return DEFAULT_VALUE;

			Result result;
			try {
				//SQLファイルが存在するかチェックするために実行
				result = methodExtractor.execute(e);
			} catch (SqlNotFoundException | SqlFileNotFoundException ex) {
				error(ex.getMessage(), e);

				metadataBuilder.setError();

				return DEFAULT_VALUE;
			}

			if (info.parametersUnfolder == null) {
				//通常のメソッド引数が存在するので検査対象
				Set<String> placeholders = new TreeSet<>();
				PlaceholderFinder.execute(result.sql, f -> {
					placeholders.add(f.placeholder);
				});

				if (!new TreeSet<>(info.parameterNames).equals(placeholders)) {
					//SQLのプレースホルダーがパラメータの名前と一致しません
					error("SQL placeholders do not match parameter names", e);

					metadataBuilder.setError();

					return DEFAULT_VALUE;
				}
			}

			var columns = new LinkedList<Found>();
			ColumnFinder.execute(result.sql, columns::add);

			var returnTypeChecker = new ReturnTypeChecker(!columns.isEmpty());

			var returnTypeCheckerResult = returnType.accept(returnTypeChecker, e);
			if (returnTypeCheckerResult.retry) {
				returnTypeCheckerResult = e.getReturnType().accept(returnTypeChecker, e);
			}

			if (returnTypeCheckerResult.dataType != null) {
				info.dataType = returnTypeCheckerResult.dataType.accept(typeNameExtractor, e);
			}

			if (returnTypeCheckerResult.protoatomUnfolderType != null) {
				info.protoatomUnfolder = returnTypeCheckerResult.protoatomUnfolderType.accept(typeNameExtractor, e);
			}

			p.add(info);

			return DEFAULT_VALUE;
		}
	}
}

package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic.Kind;

import jp.ats.atomsql.AtomSqlInitializer;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.processor.MetadataBuilder.MethodInfo;
import jp.ats.atomsql.processor.MetadataBuilder.MethodVisitor;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes("jp.ats.atomsql.annotation.DataObject")
@SupportedSourceVersion(SourceVersion.RELEASE_16)
public class DataObjectAnnotationProcessor extends AbstractProcessor {

	private final AtomSqlTypeFactory typeFactory;

	private final TypeNameExtractor typeNameExtractor = new TypeNameExtractor(() -> DataObjectAnnotationProcessor.super.processingEnv);

	private final DataObjectAnnotationProcessorMethodVisitor methodVisitor = new DataObjectAnnotationProcessorMethodVisitor();

	private final MetadataBuilder builder = new MetadataBuilder(() -> super.processingEnv, methodVisitor);

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	static {
		AtomSqlInitializer.initializeIfUninitialized();
	}

	/**
	 * 
	 */
	public DataObjectAnnotationProcessor() {
		typeFactory = AtomSqlTypeFactory.newInstanceForProcessor(AtomSqlInitializer.configure().typeFactoryClass());
	}

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

				var elements = e.getEnclosedElements();

				var detector = new ResultSetConstructorTypeDetector();
				for (var enc : elements) {
					if (enc.accept(detector, null)) {
						if (kind != ElementKind.RECORD) {
							//パラメータがResultSetのみのコンストラクタがありrecordではない場合、フィールドはどのような型でも自由なので検査しない
							return;
						}
					}
				}

				var visitor = new MyVisitor();
				List<Element> recordConstructors = new LinkedList<>();
				elements.forEach(enc -> {
					enc.accept(visitor, recordConstructors);
				});

				//レコードの場合、コンストラクタの引数名称を保存
				if (kind == ElementKind.RECORD) {
					if (recordConstructors.size() > 1) {
						//レコードのコンストラクタが複数ある場合、パラメーター名がarg0, arg1のようになってしまうため、単一であることを強制する
						//レコードのコンストラクタは単一である必要があります
						recordConstructors.forEach(c -> {
							//EclipseではrecordにErrorを設定できないので余計なコンストラクタ側にErrorを設定する
							error("There must be a single constructor of record", c);
						});

						//EclipseではrecordにErrorを設定できないが、将来修正された時の為に一応Errorを設定しておく
						error("There must be a single constructor of record", e);

						return;
					}

					builder.build(e);
				}

				if (visitor.resultTypeChecker.optionals.size() > 0) {
					buildDataObjectMetadata(e, visitor.resultTypeChecker.optionals);
				}
			});
		});

		return false;
	}

	void buildDataObjectMetadata(Element e, Map<Name, TypeElement> optionals) {
		var elements = processingEnv.getElementUtils();
		var packageName = elements.getPackageOf(e).getQualifiedName().toString();
		var binaryName = elements.getBinaryName(ProcessorUtils.toTypeElement(e)).toString();

		var packageNameLength = packageName.length();
		var isPackageNameLengthZero = packageNameLength == 0;
		var className = binaryName.substring(isPackageNameLengthZero ? 0 : packageNameLength + 1) + Constants.DATA_OBJECT_METADATA_CLASS_SUFFIX;

		var fileName = isPackageNameLengthZero ? className : packageName + "." + className;

		if (alreadyCreatedFiles.contains(fileName))
			return;

		var template = Formatter.readTemplate(AtomSqlDataObjectMetadataTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		param.put("PROCESSOR", DataObjectAnnotationProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("INTERFACE", className);

		var optionalsPart = String.join(
			", ",
			optionals.entrySet().stream().map(DataObjectAnnotationProcessor::optionalsPart).toList());

		template = Formatter.erase(template, optionalsPart.isEmpty());

		param.put("OPTIONAL_DATAS", optionalsPart);

		template = Formatter.format(template, param);

		try {
			try (var output = new BufferedOutputStream(processingEnv.getFiler().createSourceFile(fileName, e).openOutputStream())) {
				output.write(template.getBytes(Constants.CHARSET));
			}
		} catch (IOException ioe) {
			error(ioe.getMessage(), e);
		}

		alreadyCreatedFiles.add(fileName);
	}

	private static String optionalsPart(Map.Entry<Name, TypeElement> entry) {
		return "@OptionalData(name = \"" + entry.getKey() + "\", type = " + entry.getValue().getQualifiedName() + ".class)";
	}

	private class ResultSetConstructorTypeDetector extends SimpleElementVisitor14<Boolean, Void> {

		@Override
		protected Boolean defaultAction(Element e, Void p) {
			return false;
		}

		@Override
		public Boolean visitExecutable(ExecutableElement e, Void p) {
			//コンストラクタ以外はスキップ
			if (!"<init>".equals(e.getSimpleName().toString()))
				return false;

			var params = e.getParameters();

			if (params.size() != 1) return false;

			//パラメータがResultSetのみのコンストラクタはOK
			if (params.get(0).asType().accept(ParameterTypeIsResultSetChecker.instance, null)) return true;

			return false;
		}
	}

	private class MyVisitor extends SimpleElementVisitor14<Boolean, List<Element>> {

		private final ResultTypeChecker resultTypeChecker = new ResultTypeChecker();

		@Override
		public Boolean visitExecutable(ExecutableElement e, List<Element> p) {
			//コンストラクタ以外はスキップ
			if (!"<init>".equals(e.getSimpleName().toString()))
				return true;

			//レコードのコンストラクタの場合はスキップ
			if (e.getEnclosingElement().getKind() == ElementKind.RECORD) {
				//レコードコンストラクタの数を数える目的で追加
				p.add(e);
				return true;
			}

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

			return true;
		}

		@Override
		public Boolean visitRecordComponent(RecordComponentElement e, List<Element> p) {
			return e.asType().accept(resultTypeChecker, e);
		}

		@Override
		public Boolean visitVariable(VariableElement e, List<Element> p) {
			return e.asType().accept(resultTypeChecker, e);
		}
	}

	private class ResultTypeChecker extends SimpleTypeVisitor14<Boolean, Element> {

		private final Map<Name, TypeElement> optionals = new LinkedHashMap<>();

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
			var type = ProcessorUtils.toTypeElement(t.asElement());

			if (ProcessorUtils.sameClass(type, Optional.class)) {
				var arg = t.getTypeArguments().get(0);

				if (arg.getKind() == TypeKind.WILDCARD) {
					return defaultAction(t, p);
				}

				var argType = ProcessorUtils.toTypeElement(ProcessorUtils.toElement(arg));
				if (!typeFactory.canUseForResult(argType))
					return defaultAction(t, p);

				optionals.put(p.getSimpleName(), argType);

				return true;
			}

			if (typeFactory.canUseForResult(type))
				return true;

			return defaultAction(t, p);
		}
	}

	private static class ParameterTypeIsResultSetChecker extends SimpleTypeVisitor14<Boolean, Void> {

		private static final ParameterTypeIsResultSetChecker instance = new ParameterTypeIsResultSetChecker();

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
		processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}
}

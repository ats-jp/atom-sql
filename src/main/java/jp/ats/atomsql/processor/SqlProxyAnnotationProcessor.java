package jp.ats.atomsql.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic.Kind;

import jp.ats.atomsql.Atom;
import jp.ats.atomsql.Constants;
import jp.ats.atomsql.ParameterType;
import jp.ats.atomsql.Utils;
import jp.ats.atomsql.annotation.DataObject;
import jp.ats.atomsql.annotation.SqlParameter;
import jp.ats.atomsql.annotation.SqlProxy;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes("jp.ats.atomsql.annotation.SqlProxy")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class SqlProxyAnnotationProcessor extends AbstractProcessor {

	private static final ThreadLocal<Boolean> hasError = ThreadLocal.withInitial(() -> false);

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	private final TypeNameExtractor typeNameExtractor = new TypeNameExtractor();

	private final MethodVisitor methodVisitor = new MethodVisitor();

	private final ReturnTypeChecker returnTypeChecker = new ReturnTypeChecker();

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			annotations.forEach(a -> {
				roundEnv.getElementsAnnotatedWith(a).forEach(this::execute);
			});
		} catch (ProcessException e) {
			return false;
		}

		return true;
	}

	private void execute(Element e) {
		ElementKind kind = e.getKind();
		if (kind != ElementKind.INTERFACE) {
			error("cannot annotate" + kind.name() + " with " + SqlProxy.class.getSimpleName(), e);

			throw new ProcessException();
		}

		List<MethodInfo> infos = new LinkedList<>();

		hasError.set(false);

		e.getEnclosedElements().forEach(enc -> {
			enc.accept(methodVisitor, infos);
		});

		if (hasError.get()) {
			return;
		}

		String template = Formatter.readTemplate(AtomSqlMetadataTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		String packageName = packageName(e);

		String className = className(packageName, e) + Constants.METADATA_CLASS_SUFFIX;

		String fileName = packageName.isEmpty() ? className : packageName + "." + className;

		if (alreadyCreatedFiles.contains(fileName))
			return;

		param.put("PROCESSOR", SqlProxyAnnotationProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("INTERFACE", className);

		String methodPart = buildMetodsPart(infos);

		template = Formatter.erase(template, methodPart.isEmpty());

		param.put("METHODS", methodPart);

		template = Formatter.format(template, param);

		try {
			try (Writer writer = super.processingEnv.getFiler().createSourceFile(fileName, e).openWriter()) {
				writer.write(template);
			}
		} catch (IOException ioe) {
			error(ioe.getMessage(), e);
		}

		alreadyCreatedFiles.add(fileName);

		info(fileName + " generated");
	}

	private String packageName(Element e) {
		return super.processingEnv.getElementUtils().getPackageOf(e).getQualifiedName().toString();
	}

	private String className(String packageName, Element element) {
		TypeElement type = element.accept(TypeConverter.instance, null);
		String name = type.getQualifiedName().toString();

		name = name.substring(packageName.length());

		name = name.replaceFirst("^\\.", "");

		return name.replace('.', '$');
	}

	private static String buildMetodsPart(List<MethodInfo> infos) {
		return String.join(", ",
				infos.stream().map(SqlProxyAnnotationProcessor::methodPart).collect(Collectors.toList()));
	}

	private static String methodPart(MethodInfo info) {
		String args = String.join(", ",
				info.parameterNames.stream().map(n -> "\"" + n + "\"").collect(Collectors.toList()));

		String types = String.join(", ",
				info.parameterTypes.stream().map(t -> t + ".class").collect(Collectors.toList()));

		var methodContents = new LinkedList<String>();
		methodContents.add("name = \"" + info.name + "\"");
		methodContents.add("args = {" + args + "}");
		methodContents.add("argTypes = {" + types + "}");

		if (info.sqlParameterClassName != null)
			methodContents.add("sqlParameterClass = " + info.sqlParameterClassName + ".class");

		if (info.returnTypeParameterClassName != null)
			methodContents.add("dataObjectClass = " + info.returnTypeParameterClassName + ".class");

		return "@Method(" + String.join(", ", methodContents) + ")";
	}

	private void info(String message) {
		super.processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	private void error(String message, Element e) {
		super.processingEnv.getMessager().printMessage(Kind.ERROR, message, e);
	}

	private class ReturnTypeChecker extends SimpleTypeVisitor8<TypeMirror, ExecutableElement> {

		@Override
		protected TypeMirror defaultAction(TypeMirror e, ExecutableElement p) {
			error("cannot use return type [" + e + "]", p);
			hasError.set(true);
			return DEFAULT_VALUE;
		}

		@Override
		public TypeMirror visitPrimitive(PrimitiveType t, ExecutableElement p) {
			switch (t.getKind()) {
			case INT:
				return DEFAULT_VALUE;
			default:
				return defaultAction(t, p);
			}
		}

		@Override
		public TypeMirror visitDeclared(DeclaredType t, ExecutableElement p) {
			var type = t.asElement().accept(TypeConverter.instance, null);

			if (!ProcessorUtils.sameClass(type, Atom.class) && !ProcessorUtils.sameClass(type, Stream.class)
					&& !ProcessorUtils.sameClass(type, List.class))
				return defaultAction(t, p);

			var typeArg = t.getTypeArguments().get(0);
			if (typeArg.accept(AnnotationExtractor.instance, null))
				return typeArg;

			// <?>
			return null;
		}
	}

	private class ParameterTypeChecker extends SimpleTypeVisitor8<TypeMirror, VariableElement> {

		private final ExecutableElement method;

		private ParameterTypeChecker(ExecutableElement method) {
			this.method = method;
		}

		@Override
		protected TypeMirror defaultAction(TypeMirror e, VariableElement p) {
			error("cannot use parameter type [" + e + "]", p);
			hasError.set(true);
			return DEFAULT_VALUE;
		}

		@Override
		public TypeMirror visitPrimitive(PrimitiveType t, VariableElement p) {
			switch (t.getKind()) {
			case BOOLEAN:
			case BYTE:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
				return DEFAULT_VALUE;
			default:
				return defaultAction(t, p);
			}
		}

		@Override
		public TypeMirror visitDeclared(DeclaredType t, VariableElement p) {
			TypeElement type = t.asElement().accept(TypeConverter.instance, null);

			if (ProcessorUtils.sameClass(type, Consumer.class)) {
				return processConsumerType(p);
			}

			if (ProcessorUtils.sameClass(type, ParameterType.BIG_DECIMAL.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.BINARY_STREAM.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.BLOB.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.BYTE_ARRAY.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.CHARACTER_STREAM.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.CLOB.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.STRING.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, ParameterType.TIMESTAMP.type()))
				return DEFAULT_VALUE;

			return defaultAction(t, p);
		}

		public TypeMirror visitError(ErrorType t, VariableElement p) {
			TypeElement type = t.asElement().accept(TypeConverter.instance, null);

			if (ProcessorUtils.sameClass(type, Consumer.class)) {
				return processConsumerType(p);
			}

			return defaultAction(t, p);
		}

		private TypeMirror processConsumerType(VariableElement p) {
			var typeArg = p.asType().accept(TypeArgumentsExtractor.instance, null).get(0);

			var element = typeArg.accept(ElementConverter.instance, p);
			if (element == null) {
				error("unknown error occurred", p);
				hasError.set(true);
				return DEFAULT_VALUE;
			}

			var clazz = element.accept(TypeConverter.instance, null);
			var className = clazz.getQualifiedName().toString();

			var packageName = ProcessorUtils.getPackageElement(clazz).getQualifiedName().toString();

			var typeName = Utils.extractSimpleClassName(className, packageName);

			var annotation = method.getAnnotation(SqlParameter.class);
			if (annotation == null) {
				error("Consumer needs " + SqlParameter.class.getSimpleName() + " annotation", p);
				hasError.set(true);
				return DEFAULT_VALUE;
			}

			var annotationValue = annotation.value();

			if (!typeName.equals(annotationValue)) {
				error("[" + annotationValue + "] and [" + typeName + "] do not match", p);
				hasError.set(true);
				return DEFAULT_VALUE;
			}

			return typeArg;
		}
	}

	private class MethodVisitor extends SimpleElementVisitor8<Void, List<MethodInfo>> {

		@Override
		public Void visitExecutable(ExecutableElement e, List<MethodInfo> p) {
			if (e.getModifiers().contains(Modifier.DEFAULT)) {
				error("cannot use default method", e);
				throw new ProcessException();
			}

			MethodInfo info = new MethodInfo();

			ParameterTypeChecker checker = new ParameterTypeChecker(e);

			info.name = e.getSimpleName().toString();
			e.getParameters().forEach(parameter -> {
				var typeArg = parameter.asType().accept(checker, parameter);

				info.parameterNames.add(parameter.getSimpleName().toString());

				info.parameterTypes.add(parameter.asType().accept(typeNameExtractor, e));

				if (typeArg != null)
					info.sqlParameterClassName = typeArg.accept(typeNameExtractor, e);
			});

			var returnTypeParameter = e.getReturnType().accept(returnTypeChecker, e);

			if (returnTypeParameter != null) {
				if (returnTypeParameter.getAnnotation(DataObject.class) == null) {
					error(Atom.class.getSimpleName() + "'s type parameter must be annotated with "
							+ DataObject.class.getSimpleName(), e);
				} else {
					info.returnTypeParameterClassName = returnTypeParameter.accept(typeNameExtractor, e);
				}
			}

			p.add(info);

			return DEFAULT_VALUE;
		}
	}

	private class TypeNameExtractor extends SimpleTypeVisitor8<String, Element> {

		@Override
		protected String defaultAction(TypeMirror e, Element p) {
			error("unknown error occurred", p);
			return DEFAULT_VALUE;
		}

		@Override
		public String visitPrimitive(PrimitiveType t, Element p) {
			switch (t.getKind()) {
			case BOOLEAN:
				return boolean.class.getName();
			case BYTE:
				return byte.class.getName();
			case SHORT:
				return short.class.getName();
			case INT:
				return int.class.getName();
			case LONG:
				return long.class.getName();
			case CHAR:
				return char.class.getName();
			case FLOAT:
				return float.class.getName();
			case DOUBLE:
				return double.class.getName();
			default:
				return defaultAction(t, p);
			}
		}

		@Override
		public String visitDeclared(DeclaredType t, Element p) {
			return t.asElement().accept(TypeConverter.instance, null).getQualifiedName().toString();
		}

		// Consumer<SqlParameter>等型パラメータのあるものがここに来る
		@Override
		public String visitError(ErrorType t, Element p) {
			return t.asElement().accept(TypeConverter.instance, null).getQualifiedName().toString();
		}
	}

	private static class ElementConverter extends SimpleTypeVisitor8<Element, Element> {

		private static ElementConverter instance = new ElementConverter();

		@Override
		protected Element defaultAction(TypeMirror e, Element p) {
			return DEFAULT_VALUE;
		}

		@Override
		public Element visitDeclared(DeclaredType t, Element p) {
			return t.asElement();
		}

		@Override
		public Element visitError(ErrorType t, Element p) {
			return t.asElement();
		}
	}

	private static class AnnotationExtractor extends SimpleTypeVisitor8<Boolean, ExecutableElement> {

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

	private static class TypeArgumentsExtractor extends SimpleTypeVisitor8<List<? extends TypeMirror>, Void> {

		private static final TypeArgumentsExtractor instance = new TypeArgumentsExtractor();

		@Override
		protected List<? extends TypeMirror> defaultAction(TypeMirror e, Void p) {
			return null;
		}

		@Override
		public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void p) {
			return t.getTypeArguments();
		}

		@Override
		public List<? extends TypeMirror> visitError(ErrorType t, Void p) {
			return t.getTypeArguments();
		}
	}

	private static class MethodInfo {

		private String name;

		private final List<String> parameterNames = new LinkedList<>();

		private final List<String> parameterTypes = new LinkedList<>();

		private String sqlParameterClassName;

		private String returnTypeParameterClassName;
	}
}

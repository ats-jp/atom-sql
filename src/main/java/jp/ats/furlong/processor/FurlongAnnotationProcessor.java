package jp.ats.furlong.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic.Kind;

import jp.ats.furlong.Binder;
import jp.ats.furlong.DataObject;
import jp.ats.furlong.Furlong;
import jp.ats.furlong.SQLProxy;

/**
 * @author 千葉 哲嗣
 */
@SupportedAnnotationTypes("jp.ats.furlong.SQLProxy")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class FurlongAnnotationProcessor extends AbstractProcessor {

	private static final TypeConverter typeConverter = new TypeConverter();

	private static final ThreadLocal<Boolean> hasError = ThreadLocal.withInitial(() -> false);

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.size() == 0)
			return false;

		try {
			annotations.forEach(a -> {
				roundEnv.getElementsAnnotatedWith(a).forEach(e -> {
					ElementKind kind = e.getKind();
					if (kind != ElementKind.INTERFACE) {
						error("cannot annotate" + kind.name() + " with " + SQLProxy.class.getSimpleName(), e);

						throw new ProcessException();
					}

					List<MethodInfo> infos = new LinkedList<>();

					hasError.set(false);

					MethodVisitor visitor = new MethodVisitor();
					e.getEnclosedElements().forEach(enc -> {
						enc.accept(visitor, infos);
					});

					if (hasError.get()) {
						return;
					}

					String template = Formatter.readTemplate(FurlongMetadataTemplate.class, "UTF-8");
					template = Formatter.convertToTemplate(template);

					Map<String, String> param = new HashMap<>();

					String packageName = packageName(e);

					String className = className(packageName, e) + Furlong.METADATA_CLASS_SUFFIX;

					param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
					param.put("INTERFACE", className);

					String methodPart = buildMetodsPart(infos);

					template = Formatter.erase(template, methodPart.isEmpty());

					param.put("METHODS", methodPart);

					template = Formatter.format(template, param);

					String fileName = packageName.isEmpty() ? className : packageName + "." + className;
					try {
						try (Writer writer = super.processingEnv.getFiler().createSourceFile(fileName).openWriter()) {
							writer.write(template);
						}
					} catch (IOException ioe) {
						error(ioe.getMessage(), e);
					}

					info(fileName + " generated");
				});
			});
		} catch (ProcessException e) {
			return false;
		}

		return true;
	}

	private String packageName(Element e) {
		return super.processingEnv.getElementUtils().getPackageOf(e).getQualifiedName().toString();
	}

	private String className(String packageName, Element element) {
		TypeElement type = element.accept(typeConverter, null);
		String name = type.getQualifiedName().toString();

		name = name.substring(packageName.length());

		name = name.replaceFirst("^\\.", "");

		return name.replace('.', '$');
	}

	private static String buildMetodsPart(List<MethodInfo> infos) {
		return String.join(", ",
				infos.stream().map(FurlongAnnotationProcessor::methodPart).collect(Collectors.toList()));
	}

	private static String methodPart(MethodInfo info) {
		String args = String.join(", ",
				info.parameterNames.stream().map(n -> "\"" + n + "\"").collect(Collectors.toList()));

		String types = String.join(", ",
				info.parameterTypes.stream().map(t -> t + ".class").collect(Collectors.toList()));

		if (info.returnTypeArgumantClassName != null)
			return "@Method(name = \"" + info.name + "\", args = {" + args + "}, argTypes = {" + types
					+ "}, dataObjectClass = " + info.returnTypeArgumantClassName + ".class)";

		return "@Method(name = \"" + info.name + "\", args = {" + args + "}, argTypes = {" + types + "})";
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
			TypeElement type = t.asElement().accept(typeConverter, null);

			if (!ProcessorUtils.sameClass(type, Stream.class))
				return defaultAction(t, p);

			var typeArg = t.getTypeArguments().get(0);
			if (typeArg.accept(new AnnotationExtractor(), null))
				return typeArg;

			return defaultAction(t, p);
		}

		@Override
		public TypeMirror visitNoType(NoType t, ExecutableElement p) {
			// void
			return DEFAULT_VALUE;
		}
	}

	private class ParameterTypeChecker extends SimpleTypeVisitor8<Void, VariableElement> {

		@Override
		protected Void defaultAction(TypeMirror e, VariableElement p) {
			error("cannot use parameter type [" + e + "]", p);
			hasError.set(true);
			return DEFAULT_VALUE;
		}

		@Override
		public Void visitPrimitive(PrimitiveType t, VariableElement p) {
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
		public Void visitDeclared(DeclaredType t, VariableElement p) {
			TypeElement type = t.asElement().accept(typeConverter, null);

			if (ProcessorUtils.sameClass(type, Binder.BIG_DECIMAL.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.BINARY_STREAM.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.BLOB.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.BYTE_ARRAY.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.CHARACTER_STREAM.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.CLOB.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.STRING.type()))
				return DEFAULT_VALUE;
			if (ProcessorUtils.sameClass(type, Binder.TIMESTAMP.type()))
				return DEFAULT_VALUE;

			return defaultAction(t, p);
		}
	}

	private class MethodVisitor extends SimpleElementVisitor8<Void, List<MethodInfo>> {

		@Override
		public Void visitExecutable(ExecutableElement e, List<MethodInfo> p) {
			if (e.getModifiers().contains(Modifier.DEFAULT)) {
				error("cannot use default method", e);
				throw new ProcessException();
			}

			var returnTypeArg = e.getReturnType().accept(new ReturnTypeChecker(), e);

			MethodInfo info = new MethodInfo();

			ParameterTypeChecker checker = new ParameterTypeChecker();

			info.name = e.getSimpleName().toString();
			e.getParameters().forEach(parameter -> {
				parameter.asType().accept(checker, parameter);

				info.parameterNames.add(parameter.getSimpleName().toString());

				info.parameterTypes.add(parameter.asType().accept(new TypeNameExtractor(), null));
			});

			if (returnTypeArg != null) {
				info.returnTypeArgumantClassName = returnTypeArg.accept(new TypeNameExtractor(), e);
			}

			p.add(info);

			return DEFAULT_VALUE;
		}
	}

	private class TypeNameExtractor extends SimpleTypeVisitor8<String, ExecutableElement> {

		@Override
		protected String defaultAction(TypeMirror e, ExecutableElement p) {
			error("unknown error occurred", p);
			return DEFAULT_VALUE;
		}

		@Override
		public String visitPrimitive(PrimitiveType t, ExecutableElement p) {
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
		public String visitDeclared(DeclaredType t, ExecutableElement p) {
			return t.asElement().accept(typeConverter, null).getQualifiedName().toString();
		}
	}

	private class AnnotationExtractor extends SimpleTypeVisitor8<Boolean, ExecutableElement> {

		@Override
		protected Boolean defaultAction(TypeMirror e, ExecutableElement p) {
			return false;
		}

		@Override
		public Boolean visitDeclared(DeclaredType t, ExecutableElement p) {
			return t.asElement().getAnnotation(DataObject.class) != null;
		}
	}

	private static class MethodInfo {

		private String name;

		private final List<String> parameterNames = new LinkedList<>();

		private final List<String> parameterTypes = new LinkedList<>();

		private String returnTypeArgumantClassName;
	}
}

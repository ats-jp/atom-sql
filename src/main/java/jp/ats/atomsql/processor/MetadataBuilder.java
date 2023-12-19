package jp.ats.atomsql.processor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.SimpleElementVisitor14;
import javax.tools.Diagnostic.Kind;

import jp.ats.atomsql.Constants;

class MetadataBuilder {

	private boolean hasError = false;

	// 二重作成防止チェッカー
	// 同一プロセス内でプロセッサのインスタンスが変わる場合はこの方法では防げないので、その場合は他の方法を検討
	private final Set<String> alreadyCreatedFiles = new HashSet<>();

	private final Supplier<ProcessingEnvironment> envSupplier;

	private final MethodVisitor visitor;

	static class MethodVisitor extends SimpleElementVisitor14<Void, List<MethodInfo>> {}

	MetadataBuilder(Supplier<ProcessingEnvironment> envSupplier, MethodVisitor visitor) {
		this.envSupplier = envSupplier;
		this.visitor = visitor;
	}

	void setError() {
		hasError = true;
	}

	boolean hasError() {
		return hasError;
	}

	void build(Element e) {
		List<MethodInfo> infos = new LinkedList<>();

		hasError = false;

		try {
			e.getEnclosedElements().forEach(enc -> {
				enc.accept(visitor, infos);
			});
		} catch (Exception ex) {
			//EclipseでAbortCompilationが発生することへの対応
			error(ProcessorUtils.message(ex), e);

			return;
		}

		if (hasError) {
			return;
		}

		var template = Formatter.readTemplate(AtomSqlMetadataTemplate.class, "UTF-8");
		template = Formatter.convertToTemplate(template);

		Map<String, String> param = new HashMap<>();

		var packageName = packageName(e);

		var binaryName = envSupplier.get().getElementUtils().getBinaryName(ProcessorUtils.toTypeElement(e)).toString();

		var packageNameLength = packageName.length();
		var isPackageNameLengthZero = packageNameLength == 0;
		var className = binaryName.substring(isPackageNameLengthZero ? 0 : packageNameLength + 1) + Constants.METADATA_CLASS_SUFFIX;

		var fileName = isPackageNameLengthZero ? className : packageName + "." + className;

		if (alreadyCreatedFiles.contains(fileName))
			return;

		param.put("PROCESSOR", SqlProxyProcessor.class.getName());

		param.put("PACKAGE", packageName.isEmpty() ? "" : ("package " + packageName + ";"));
		param.put("INTERFACE", className);

		var methodPart = String.join(
			", ",
			infos.stream().map(MetadataBuilder::methodPart).toList());

		template = Formatter.erase(template, methodPart.isEmpty());

		param.put("METHODS", methodPart);

		template = Formatter.format(template, param);

		try {
			try (var output = new BufferedOutputStream(envSupplier.get().getFiler().createSourceFile(fileName, e).openOutputStream())) {
				output.write(template.getBytes(Constants.CHARSET));
			}
		} catch (IOException ioe) {
			error(ioe.getMessage(), e);
		}

		alreadyCreatedFiles.add(fileName);
	}

	private static String methodPart(MethodInfo info) {
		var parameters = String.join(
			", ",
			info.parameterNames.stream().map(n -> "\"" + n + "\"").toList());

		var types = String.join(
			", ",
			info.parameterTypes.stream().map(t -> t + ".class").collect(Collectors.toList()));

		var methodContents = new LinkedList<String>();
		methodContents.add("name = \"" + info.name + "\"");
		methodContents.add("parameters = {" + parameters + "}");
		methodContents.add("parameterTypes = {" + types + "}");

		if (info.sqlParameters != null)
			methodContents.add("sqlParameters = " + info.sqlParameters + ".class");

		if (info.dataObject != null)
			methodContents.add("dataObject = " + info.dataObject + ".class");

		if (info.sqlInterpolation != null)
			methodContents.add("sqlInterpolation = " + info.sqlInterpolation + ".class");

		if (info.sqlProxy != null)
			methodContents.add("sqlProxy = " + info.sqlProxy + ".class");

		return "@Method(" + String.join(", ", methodContents) + ")";
	}

	private String packageName(Element e) {
		return envSupplier.get().getElementUtils().getPackageOf(e).getQualifiedName().toString();
	}

	private void error(String message, Element e) {
		envSupplier.get().getMessager().printMessage(Kind.ERROR, message, e);
	}

	static class MethodInfo {

		String name;

		final List<String> parameterNames = new LinkedList<>();

		final List<String> parameterTypes = new LinkedList<>();

		String sqlParameters;

		String dataObject;

		String sqlInterpolation;

		String sqlProxy;
	}
}

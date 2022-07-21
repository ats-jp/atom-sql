package jp.ats.atomsql.processor;

class MethodInfo {

	final String generatedClass;

	final String enclosingClass;

	final String method;

	MethodInfo(String line) {
		var splitted = line.split("/");
		generatedClass = splitted[0];
		enclosingClass = splitted[1];
		method = splitted[2];
	}

	MethodInfo(String parametersClass, String clazz, String method) {
		this.generatedClass = parametersClass;
		this.enclosingClass = clazz;
		this.method = method;
	}

	String pack() {
		return generatedClass + "/" + enclosingClass + "/" + method;
	}
}

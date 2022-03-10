package jp.ats.atomsql.processor;

import java.util.function.Supplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic.Kind;

class TypeNameExtractor extends SimpleTypeVisitor14<String, Element> {

	private final Supplier<ProcessingEnvironment> envSupplier;

	TypeNameExtractor(Supplier<ProcessingEnvironment> envSupplier) {
		this.envSupplier = envSupplier;
	}

	@Override
	protected String defaultAction(TypeMirror e, Element p) {
		//不明なエラーが発生しました
		error("Unknown error occurred", p);
		return DEFAULT_VALUE;
	}

	@Override
	public String visitPrimitive(PrimitiveType t, Element p) {
		return switch (t.getKind()) {
		case BOOLEAN -> boolean.class.getName();
		case BYTE -> byte.class.getName();
		case SHORT -> short.class.getName();
		case INT -> int.class.getName();
		case LONG -> long.class.getName();
		case CHAR -> char.class.getName();
		case FLOAT -> float.class.getName();
		case DOUBLE -> double.class.getName();
		default -> defaultAction(t, p);
		};
	}

	@Override
	public String visitArray(ArrayType t, Element p) {
		return t.getComponentType().accept(this, p) + "[]";
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

	private void error(String message, Element e) {
		envSupplier.get().getMessager().printMessage(Kind.ERROR, message, e);
	}
}

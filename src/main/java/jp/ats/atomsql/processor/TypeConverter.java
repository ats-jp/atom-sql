package jp.ats.atomsql.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;

/**
 * @author 千葉 哲嗣
 */
class TypeConverter extends SimpleElementVisitor8<TypeElement, Void> {

	static final TypeConverter instance = new TypeConverter();

	private TypeConverter() {
	}

	@Override
	protected TypeElement defaultAction(Element e, Void p) {
		throw new ProcessException();
	}

	@Override
	public TypeElement visitType(TypeElement e, Void p) {
		return e;
	}
}

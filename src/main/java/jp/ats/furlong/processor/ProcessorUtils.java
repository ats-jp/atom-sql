package jp.ats.furlong.processor;

import javax.lang.model.element.TypeElement;

/**
 * @author 千葉 哲嗣
 */
class ProcessorUtils {

	static boolean sameClass(TypeElement type, Class<?> clazz) {
		return type.getQualifiedName().toString().equals(clazz.getCanonicalName());
	}
}

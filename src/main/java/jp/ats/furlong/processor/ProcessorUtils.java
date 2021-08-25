package jp.ats.furlong.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor8;

/**
 * @author 千葉 哲嗣
 */
class ProcessorUtils {

	private static final PackageExtractor packageExtractor = new PackageExtractor();

	static boolean sameClass(TypeElement type, Class<?> clazz) {
		return type.getQualifiedName().toString().equals(clazz.getCanonicalName());
	}

	static PackageElement getPackageElement(TypeElement clazz) {
		Element enclosing = clazz;
		PackageElement packageElement = null;
		do {
			enclosing = enclosing.getEnclosingElement();
			packageElement = enclosing.accept(packageExtractor, null);
		} while (packageElement == null);

		return packageElement;
	}

	private static class PackageExtractor extends SimpleElementVisitor8<PackageElement, Void> {

		@Override
		protected PackageElement defaultAction(Element e, Void p) {
			return null;
		}

		@Override
		public PackageElement visitPackage(PackageElement e, Void p) {
			return e;
		}
	}
}

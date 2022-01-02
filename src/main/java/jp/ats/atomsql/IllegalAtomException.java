package jp.ats.atomsql;

import java.util.function.Function;

/**
 * {@link Atom#newStaticInstance(Function)}で生成されたインスタンスで、検索など不可能な操作を行った場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class IllegalAtomException extends RuntimeException {

	private static final long serialVersionUID = -7265663687609245022L;

	IllegalAtomException() {
		super("Cannot perform on the instance created by Atom#newStaticInstance");
	}
}

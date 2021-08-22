package jp.ats.furlong.processor.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author 千葉 哲嗣
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Methods {

	/**
	 * メソッドの引数名
	 * @return メソッドの引数名
	 */
	Method[] value();
}

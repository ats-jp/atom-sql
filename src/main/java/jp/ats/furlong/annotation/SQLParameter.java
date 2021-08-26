package jp.ats.furlong.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface SQLParameter {

	/**
	 * 自動生成されるクラス名
	 * @return パッケージを除いたクラス名
	 */
	String value();
}

package jp.ats.furlong.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author 千葉 哲嗣
 */
@Target({ METHOD })
@Retention(RUNTIME)
public @interface SqlParameter {

	/**
	 * 自動生成されるクラス名
	 * @return パッケージを除いたクラス名
	 */
	String value();
}

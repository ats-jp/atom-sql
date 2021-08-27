package jp.ats.furlong.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jp.ats.furlong.Furlong;

/**
 * {@link Furlong} の対象となるインターフェイスであることを表すアノテーションです。
 * 
 * @author 千葉 哲嗣
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface SqlProxy {
}

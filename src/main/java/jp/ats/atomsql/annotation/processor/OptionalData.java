package jp.ats.atomsql.annotation.processor;

import java.util.Optional;

/**
 * {@link Optional}検索結果項目
 * @author 千葉 哲嗣
 */
public @interface OptionalData {

	/**
	 * フィールド名またはレコードコンポーネント名
	 * @return フィールド名またはレコードコンポーネント名
	 */
	String name();

	/**
	 * 型引数
	 * @return 型引数
	 */
	Class<?> type();
}

package jp.ats.atomsql;

/**
 * 指定されたSQL内に、パラメータ名に該当するプレースホルダが見つからない場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class PlaceholderNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -9051235404234778719L;

	PlaceholderNotFoundException(String placeholder) {
		//プレースホルダplaceholderは見つかりませんでした
		super("Place holder [" + placeholder + "] was not found");
	}
}

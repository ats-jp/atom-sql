package jp.ats.atomsql;

/**
 * 検索で取得した値から該当するenumを復元する際に対応するenum要素が見つからない場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class EnumNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7943269571833059820L;

	/**
	 * コンストラクタ
	 * @param enumClass
	 * @param value
	 */
	public EnumNotFoundException(Class<? extends Enum<?>> enumClass, Object value) {
		//値 [value] が [enumClass] に見つかりません
		super("Value [" + value + "] not found in enum [" + enumClass.getName() + "]");
	}
}

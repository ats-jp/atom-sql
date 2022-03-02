package jp.ats.atomsql;

/**
 * {@link AtomSqlType}に定義されていない型名を使用した場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class UnknownSqlTypeNameException extends RuntimeException {

	private static final long serialVersionUID = 554510706211577952L;

	private final String unknownTypeName;

	UnknownSqlTypeNameException(String unknownTypeName) {
		this.unknownTypeName = unknownTypeName;
	}

	/**
	 * この例外を発生させた不明な型名を返します。
	 * @return 不明な型名
	 */
	public String unknownTypeName() {
		return unknownTypeName;
	}
}

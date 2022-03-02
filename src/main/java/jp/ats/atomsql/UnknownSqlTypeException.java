package jp.ats.atomsql;

/**
 * {@link AtomSqlType}に定義されていない型を使用した場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class UnknownSqlTypeException extends RuntimeException {

	private static final long serialVersionUID = -5236887139627158692L;

	private final Class<?> unknownType;

	UnknownSqlTypeException(Class<?> unknownType) {
		this.unknownType = unknownType;
	}

	/**
	 * この例外を発生させた不明な型を返します。
	 * @return 不明な型
	 */
	public Class<?> unknownType() {
		return unknownType;
	}
}

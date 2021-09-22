package jp.ats.atomsql;

/**
 * {@link AtomSqlType}に定義されていない型を使用した場合に投げられる例外です。
 * @author 千葉 哲嗣
 */
public class UnknownTypeException extends RuntimeException {

	private static final long serialVersionUID = 3158990216231477861L;

	private final Class<?> unknownType;

	UnknownTypeException(Class<?> unknownType) {
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

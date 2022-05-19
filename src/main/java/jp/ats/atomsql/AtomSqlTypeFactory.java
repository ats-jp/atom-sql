package jp.ats.atomsql;

/**
 * @author 千葉 哲嗣
 */
public interface AtomSqlTypeFactory {

	/**
	 * 渡されたオブジェクトの型から対応する{@link AtomSqlType}を返します。
	 * @param o 対象となるオブジェクト
	 * @return {@link AtomSqlType}
	 */
	AtomSqlType selectForPreparedStatement(Object o);

	/**
	* 渡されたクラスから対応する{@link AtomSqlType}を返します。
	* @param c 対象となるクラス
	* @return {@link AtomSqlType}
	* @throws UnknownSqlTypeException {@link AtomSqlType}に定義されていない型を使用した場合
	*/
	AtomSqlType selectForResultSet(Class<?> c);

	/**
	 * クラス名をもとに{@link AtomSqlType}のインスタンスを返します。
	 * @param name {@link AtomSqlType}クラス名
	 * @return {@link AtomSqlType}
	 * @throws UnknownSqlTypeNameException 対応する型が存在しない場合
	 */
	AtomSqlType typeOf(String name);

	/**
	 * 型パラメータとしてこのenumが使用される場合のインスタンスを返します。
	 * @param name {@link AtomSqlType}enum名
	 * @return {@link AtomSqlType}
	 * @throws UnknownSqlTypeNameException 対応する型が存在しない場合
	 */
	AtomSqlType typeArgumentOf(String name);

	/**
	 * プリミティブ型ではない使用可能な型の実際のクラス配列を返します。
	 * @return プリミティブ型ではない使用可能な型の配列
	 */
	Class<?>[] nonPrimitiveTypes();

	/**
	 * 現在設定されているインスタンスを返します。
	 * @return {@link AtomSqlTypeFactory}
	 */
	public static AtomSqlTypeFactory instance() {
		return AtomSqlInitializer.configure().atomSqlTypeFactory();
	}
}

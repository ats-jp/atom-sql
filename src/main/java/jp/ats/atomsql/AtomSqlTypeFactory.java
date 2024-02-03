package jp.ats.atomsql;

import javax.lang.model.element.TypeElement;

/**
 * @author 千葉 哲嗣
 */
public interface AtomSqlTypeFactory {

	/**
	* 渡されたクラスから対応する{@link AtomSqlType}を返します。
	* @param c 対象となるクラス
	* @return {@link AtomSqlType}
	* @throws UnknownSqlTypeException {@link AtomSqlType}に定義されていない型を使用した場合
	*/
	AtomSqlType select(Class<?> c);

	/**
	 * タイプ名をもとに{@link AtomSqlType}のインスタンスを返します。
	 * @param name {@link AtomSqlType}タイプ名
	 * @return {@link AtomSqlType}
	 * @throws UnknownSqlTypeNameException 対応する型が存在しない場合
	 */
	AtomSqlType typeOf(String name);

	/**
	 * 型パラメータとしてこのタイプ名が使用される場合のインスタンスを返します。
	 * @param name {@link AtomSqlType}タイプ名
	 * @return {@link AtomSqlType}
	 * @throws UnknownSqlTypeNameException 対応する型が存在しない場合
	 */
	AtomSqlType typeArgumentOf(String name);

	/**
	 * プリミティブ型ではない型のうち、使用可能な型かどうかを返します。<br>
	 * {@link Csv}は、パラメータとして使用可能ですが、ここでは除外されます。
	 * @param c {@link Class}
	 * @return 使用可能な型の場合、true
	 */
	boolean canUse(Class<?> c);

	/**
	 * プリミティブ型ではない型のうち、使用可能な型かどうかを返します。
	 * {@link Csv}は、パラメータとして使用可能ですが、ここでは除外されます。
	 * @param type {@link TypeElement}
	 * @return 使用可能な型の場合、true
	 */
	boolean canUse(TypeElement type);

	/**
	 * クラス名からインスタンスを生成します。
	 * @param className {@link AtomSqlTypeFactory}を実装したクラス名
	 * @return {@link AtomSqlTypeFactory}
	 */
	public static AtomSqlTypeFactory newInstance(String className) {
		if (className == null || className.isBlank() || className.equals(DefaultAtomSqlTypeFactory.class.getName()))
			return DefaultAtomSqlTypeFactory.instance;

		try {
			return (AtomSqlTypeFactory) Class.forName(
				className,
				true,
				Thread.currentThread().getContextClassLoader()).getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * クラス名からインスタンスを生成します。<br>
	 * Annotation Processor用
	 * @param className {@link AtomSqlTypeFactory}を実装したクラス名
	 * @return {@link AtomSqlTypeFactory}
	 */
	public static AtomSqlTypeFactory newInstanceForProcessor(String className) {
		if (className == null || className.isBlank()) return DefaultAtomSqlTypeFactory.instance;
		try {
			return (AtomSqlTypeFactory) Class.forName(
				className,
				true,
				AtomSqlTypeFactory.class.getClassLoader()).getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}

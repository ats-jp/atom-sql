package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.IntStream;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.Csv;
import jp.ats.atomsql.NonThreadSafeException;
import jp.ats.atomsql.annotation.DataObject;

/**
 * Comma Separated Values<br>
 * {@link DataObject}では使用できません。
 * @see Csv
 */
public class CSV implements AtomSqlType {

	/**
	 * singleton
	 */
	public static final AtomSqlType instance = new CSV();

	private CSV() {}

	@Override
	public Class<?> type() {
		return Csv.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value, AtomSqlTypeFactory factory) {
		var values = ((Csv<?>) value).values();
		var size = values.size();
		IntStream.range(0, size).forEach(i -> {
			var v = values.get(i);

			check(factory, v);

			factory.selectForPreparedStatement(v).bind(index + i, statement, v, factory);
		});

		return index + size;
	}

	private static <T> void check(AtomSqlTypeFactory factory, Object value) {
		//Csvの中にCsvは不可
		if (value instanceof Csv) throw new IllegalArgumentException("Csv cannot be used for Csv elements");

		var type = factory.selectForPreparedStatement(value);

		if (type.nonThreadSafe()) throw new NonThreadSafeException();
	}

	@Override
	public String placeholderExpression(Object value) {
		var values = ((Csv<?>) value).values();
		return String.join(", ", values.stream().map(v -> "?").toList());
	}

	@Override
	public Object get(ResultSet rs, String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		throw new UnsupportedOperationException();
	}
}

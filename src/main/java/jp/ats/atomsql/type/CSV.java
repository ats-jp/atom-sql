package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.IntStream;

import jp.ats.atomsql.AtomSqlType;
import jp.ats.atomsql.AtomSqlTypeFactory;
import jp.ats.atomsql.Csv;
import jp.ats.atomsql.annotation.DataObject;

/**
 * Comma Separated Values<br>
 * {@link DataObject}では使用できません。
 * @see Csv
 */
public class CSV implements AtomSqlType {

	private final AtomSqlTypeFactory typeFactory;

	/**
	 * コンストラクタ
	 * @param typeFactory 値の型判定用
	 */
	public CSV(AtomSqlTypeFactory typeFactory) {
		this.typeFactory = typeFactory;
	}

	@Override
	public Class<?> type() {
		return Csv.class;
	}

	@Override
	public int bind(int index, PreparedStatement statement, Object value) {
		var values = ((Csv<?>) value).values();

		var type = typeFactory.select(values.stream().map(v -> v.getClass()).findFirst().orElseThrow(() -> new IllegalStateException()));

		var size = values.size();
		IntStream.range(0, size).forEach(i -> {
			var v = values.get(i);

			type.bind(index + i, statement, v);
		});

		return index + size;
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
	public Object get(ResultSet rs, int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public AtomSqlType toTypeArgument() {
		throw new UnsupportedOperationException();
	}
}

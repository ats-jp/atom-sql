package jp.ats.atomsql;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jp.ats.atomsql.annotation.DataObject;

/**
 * Atom SQLで使用可能な型を定義した列挙型です。<br>
 * ここで定義されている以外の型は{@link Object}型として扱われます。
 * @author 千葉 哲嗣
 */
public enum AtomSqlType {

	/**
	 * {@link BigDecimal}
	 */
	BIG_DECIMAL {

		@Override
		public Class<?> type() {
			return BigDecimal.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setBigDecimal(index, (BigDecimal) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBigDecimal(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link BinaryStream}
	 */
	BINARY_STREAM {

		@Override
		public Class<?> type() {
			return BinaryStream.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			var stream = (BinaryStream) value;
			try {
				statement.setBinaryStream(index, stream.input, stream.length);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return new BinaryStream(rs.getBinaryStream(columnLabel), -1);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link Blob}
	 */
	BLOB {

		@Override
		public Class<?> type() {
			return Blob.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setBlob(index, (Blob) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBlob(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link Boolean}
	 */
	BOOLEAN {

		@Override
		public Class<?> type() {
			return Boolean.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setBoolean(index, (boolean) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			var val = rs.getBoolean(columnLabel);
			return rs.wasNull() ? null : val;
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * プリミティブboolean
	 */
	P_BOOLEAN {

		@Override
		public Class<?> type() {
			return boolean.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			//ラッパー型が使用される
			throw new UnsupportedOperationException();
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBoolean(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return BOOLEAN;
		}
	},

	/**
	 * byte[]
	 */
	BYTE_ARRAY {

		@Override
		public Class<?> type() {
			return byte[].class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setBytes(index, (byte[]) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getBytes(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link CharacterStream}
	 */
	CHARACTER_STREAM {

		@Override
		public Class<?> type() {
			return CharacterStream.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			var stream = (CharacterStream) value;
			try {
				statement.setCharacterStream(index, stream.input, stream.length);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return new CharacterStream(rs.getCharacterStream(columnLabel), -1);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link Clob}
	 */
	CLOB {

		@Override
		public Class<?> type() {
			return Clob.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setClob(index, (Clob) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getClob(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link Double}
	 */
	DOUBLE {

		@Override
		public Class<?> type() {
			return Double.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setDouble(index, (double) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			var val = rs.getDouble(columnLabel);
			return rs.wasNull() ? null : val;
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * プリミティブdouble
	 */
	P_DOUBLE {

		@Override
		public Class<?> type() {
			return double.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			//ラッパー型が使用される
			throw new UnsupportedOperationException();
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getDouble(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return DOUBLE;
		}
	},

	/**
	 * {@link Float}
	 */
	FLOAT {

		@Override
		public Class<?> type() {
			return Float.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setFloat(index, (float) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			var val = rs.getFloat(columnLabel);
			return rs.wasNull() ? null : val;
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * プリミティブfloat
	 */
	P_FLOAT {

		@Override
		public Class<?> type() {
			return float.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			//ラッパー型が使用される
			throw new UnsupportedOperationException();
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getFloat(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return FLOAT;
		}
	},

	/**
	 * {@link Integer}
	 */
	INTEGER {

		@Override
		public Class<?> type() {
			return Integer.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setInt(index, (int) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			var val = rs.getInt(columnLabel);
			return rs.wasNull() ? null : val;
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * プリミティブint
	 */
	P_INT {

		@Override
		public Class<?> type() {
			return int.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			//ラッパー型が使用される
			throw new UnsupportedOperationException();
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getInt(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return INTEGER;
		}
	},

	/**
	 * {@link Long}
	 */
	LONG {

		@Override
		public Class<?> type() {
			return Long.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setLong(index, (long) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			var val = rs.getLong(columnLabel);
			return rs.wasNull() ? null : val;
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * プリミティブlong
	 */
	P_LONG {

		@Override
		public Class<?> type() {
			return long.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			//ラッパー型が使用される
			throw new UnsupportedOperationException();
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getLong(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return LONG;
		}
	},

	/**
	 * {@link Object}
	 */
	OBJECT {

		@Override
		public Class<?> type() {
			return Object.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setObject(index, value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getObject(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link String}
	 */
	STRING {

		@Override
		public Class<?> type() {
			return String.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setString(index, (String) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getString(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * {@link Timestamp}
	 */
	TIMESTAMP {

		@Override
		public Class<?> type() {
			return Timestamp.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			try {
				statement.setTimestamp(index, (Timestamp) value);
			} catch (SQLException e) {
				throw new AtomSqlException(e);
			}
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			return rs.getTimestamp(columnLabel);
		}

		@Override
		AtomSqlType toTypeArgument() {
			return this;
		}
	},

	/**
	 * Comma Separated Values<br>
	 * {@link DataObject}では使用できません。
	 * @see Csv
	 */
	CSV {

		@Override
		public Class<?> type() {
			return Csv.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			var values = ((Csv<?>) value).values();
			IntStream.range(0, values.size()).forEach(i -> {
				var v = values.get(i);
				selectForPreparedStatement(v).bind(index + i, statement, v);
			});
		}

		@Override
		String placeholderExpression(Object value) {
			var values = ((Csv<?>) value).values();
			return String.join(", ", values.stream().map(v -> "?").collect(Collectors.toList()));
		}

		@Override
		Object get(ResultSet rs, String columnLabel) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		AtomSqlType toTypeArgument() {
			return OBJECT;
		}
	};

	/**
	 * この型に対応するJavaでの型を返します。
	 * @return type
	 */
	public abstract Class<?> type();

	abstract void bind(int index, PreparedStatement statement, Object value);

	abstract Object get(ResultSet rs, String columnLabel) throws SQLException;

	abstract AtomSqlType toTypeArgument();

	String placeholderExpression(Object value) {
		return "?";
	}

	private static final Map<Class<?>, AtomSqlType> types = new HashMap<>();

	static {
		Arrays.stream(AtomSqlType.values()).forEach(b -> types.put(b.type(), b));
	}

	/**
	 * 渡されたオブジェクトの型から対応する{@link AtomSqlType}を返します。
	 * @param o 対象となるオブジェクト
	 * @return {@link AtomSqlType}
	 */
	public static AtomSqlType selectForPreparedStatement(Object o) {
		// nullの場合はsetObject(i, null)
		// DBによってはエラーとなる可能性があるため、その場合はsetNull(int, int)の使用を検討する
		if (o == null)
			return OBJECT;

		var type = types.get(o.getClass());
		return type == null ? OBJECT : type;
	}

	/**
	 * 渡されたクラスから対応する{@link AtomSqlType}を返します。
	 * @param c 対象となるクラス
	 * @return {@link AtomSqlType}
	 */
	public static AtomSqlType selectForResultSet(Class<?> c) {
		var type = types.get(Objects.requireNonNull(c));

		if (type == null) throw new UnknownTypeException(c);

		return type;
	}

	/**
	 * enum名をもとに{@link AtomSqlType}のインスタンスを返します。<br>
	 * 対応する型が存在しない場合はOBJECTを返します。
	 * @param name {@link AtomSqlType}enum名
	 * @return {@link AtomSqlType}
	 */
	public static AtomSqlType safeValueOf(String name) {
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return OBJECT;
		}
	}

	/**
	 * 型パラメータとしてこのenumが使用される場合のインスタンスを返します。
	 * 対応する型が存在しない場合はOBJECTを返します。
	 * @param name {@link AtomSqlType}enum名
	 * @return {@link AtomSqlType}
	 */
	public static AtomSqlType safeTypeArgumentValueOf(String name) {
		try {
			return valueOf(name).toTypeArgument();
		} catch (IllegalArgumentException e) {
			return OBJECT;
		}
	}
}

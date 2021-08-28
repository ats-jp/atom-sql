package jp.ats.atomsql;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 千葉 哲嗣
 */
public enum ParameterType {

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
	},

	/**
	 * boolean
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
	},

	/**
	 * double
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
	},

	/**
	 * float
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
	},

	/**
	 * int
	 */
	INT {

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
	},

	/**
	 * long
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
	},

	/**
	 * {@link CommaSeparatedParameters}
	 */
	COMMA_SEPARATED_PARAMETERS {

		@Override
		public Class<?> type() {
			return CommaSeparatedParameters.class;
		}

		@Override
		void bind(int index, PreparedStatement statement, Object value) {
			var values = ((CommaSeparatedParameters<?>) value).values();
			IntStream.range(0, values.size()).forEach(i -> {
				var v = values.get(i);
				select(v).bind(index + i, statement, v);
			});
		}

		@Override
		String placeholderExpression(Object value) {
			var values = ((CommaSeparatedParameters<?>) value).values();
			return String.join(", ", values.stream().map(v -> "?").collect(Collectors.toList()));
		}
	};

	/**
	 * @return type
	 */
	public abstract Class<?> type();

	abstract void bind(int index, PreparedStatement statement, Object value);

	String placeholderExpression(Object value) {
		return "?";
	}

	private static final Map<Class<?>, ParameterType> types = new HashMap<>();

	static {
		Arrays.stream(ParameterType.values()).filter(b -> !b.equals(OBJECT)).forEach(b -> types.put(b.type(), b));
	}

	/**
	 * @param o
	 * @return {@link ParameterType}
	 */
	public static ParameterType select(Object o) {
		// nullの場合はsetObject(i, null)
		// DBによってはエラーとなる可能性があるため、その場合はsetNull(int, int)の使用を検討する
		if (o == null)
			return OBJECT;

		var type = types.get(o.getClass());
		return type == null ? OBJECT : type;
	}
}

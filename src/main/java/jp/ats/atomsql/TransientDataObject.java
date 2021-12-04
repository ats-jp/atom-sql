package jp.ats.atomsql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import jp.ats.atomsql.annotation.DataObject;

/**
 * {@link ResultSet}から値を取り出すための簡易{@link DataObject}クラスです。<br>
 * 注意点として、このクラスのインスタンスは{@link ResultSet}から値を取り出してインスタンス内に保持するのではなく、各種getメソッドを呼ばれたときに{@link ResultSet}の各getメソッドを呼ぶため、検索結果を{@link List}で受け取るメソッドでは利用することができません。<br>
 * 検索結果を{@link Stream}で受け取るメソッドで使用するようにしてください。
 * @see Atom#stream()
 * @see Atom#stream(RowMapper)
 * @see Atom#stream(SimpleRowMapper)
 * @author 千葉 哲嗣
 */
@DataObject
public class TransientDataObject {

	private final ResultSet base;

	/**
	 * {@link ResultSet}を必要とする唯一のコンストラクタです。
	 * @param base {@link ResultSet}
	 */
	public TransientDataObject(ResultSet base) {
		this.base = Objects.requireNonNull(base);
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public boolean getBoolean(String columnName) {
		try {
			return base.getBoolean(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public double getDouble(String columnName) {
		try {
			return base.getDouble(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public float getFloat(String columnName) {
		try {
			return base.getFloat(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public int getInt(String columnName) {
		try {
			return base.getInt(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public long getLong(String columnName) {
		try {
			return base.getLong(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public String getString(String columnName) {
		try {
			return base.getString(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public Timestamp getTimestamp(String columnName) {
		try {
			return base.getTimestamp(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public BigDecimal getBigDecimal(String columnName) {
		try {
			return base.getBigDecimal(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public InputStream getBinaryStream(String columnName) {
		try {
			return base.getBinaryStream(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public Reader getCharacterStream(String columnName) {
		try {
			return base.getCharacterStream(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public Object getObject(String columnName) {
		try {
			return base.getObject(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public byte[] getBytes(String columnName) {
		try {
			return base.getBytes(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public Blob getBlob(String columnName) {
		try {
			return base.getBlob(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName SELECT句カラム名
	 * @return result 検索結果
	 */
	public Clob getClob(String columnName) {
		try {
			return base.getClob(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public boolean getBoolean(int columnIndex) {
		try {
			return base.getBoolean(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public double getDouble(int columnIndex) {
		try {
			return base.getDouble(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public float getFloat(int columnIndex) {
		try {
			return base.getFloat(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public int getInt(int columnIndex) {
		try {
			return base.getInt(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public long getLong(int columnIndex) {
		try {
			return base.getLong(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public String getString(int columnIndex) {
		try {
			return base.getString(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public Timestamp getTimestamp(int columnIndex) {
		try {
			return base.getTimestamp(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public BigDecimal getBigDecimal(int columnIndex) {
		try {
			return base.getBigDecimal(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public Object getObject(int columnIndex) {
		try {
			return base.getObject(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public InputStream getBinaryStream(int columnIndex) {
		try {
			return base.getBinaryStream(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public Reader getCharacterStream(int columnIndex) {
		try {
			return base.getCharacterStream(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public byte[] getBytes(int columnIndex) {
		try {
			return base.getBytes(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public Blob getBlob(int columnIndex) {
		try {
			return base.getBlob(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex SELECT句カラム位置
	 * @return result 検索結果
	 */
	public Clob getClob(int columnIndex) {
		try {
			return base.getClob(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @see ResultSet#wasNull()
	 * @return 最後に取得したカラムの値がNULLの場合、true
	 */
	public boolean wasNull() {
		try {
			return base.wasNull();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}
}

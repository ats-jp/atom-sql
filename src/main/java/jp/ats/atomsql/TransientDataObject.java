package jp.ats.atomsql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import jp.ats.atomsql.annotation.DataObject;

/**
 * @author 千葉 哲嗣
 */
@DataObject
public class TransientDataObject {

	private final ResultSet base;

	/**
	 * @param base
	 */
	public TransientDataObject(ResultSet base) {
		this.base = Objects.requireNonNull(base);
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public boolean getBoolean(String columnName) {
		try {
			return base.getBoolean(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public double getDouble(String columnName) {
		try {
			return base.getDouble(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public float getFloat(String columnName) {
		try {
			return base.getFloat(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public int getInt(String columnName) {
		try {
			return base.getInt(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public long getLong(String columnName) {
		try {
			return base.getLong(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public String getString(String columnName) {
		try {
			return base.getString(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public Timestamp getTimestamp(String columnName) {
		try {
			return base.getTimestamp(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public BigDecimal getBigDecimal(String columnName) {
		try {
			return base.getBigDecimal(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public InputStream getBinaryStream(String columnName) {
		try {
			return base.getBinaryStream(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public Reader getCharacterStream(String columnName) {
		try {
			return base.getCharacterStream(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public Object getObject(String columnName) {
		try {
			return base.getObject(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public byte[] getBytes(String columnName) {
		try {
			return base.getBytes(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public Blob getBlob(String columnName) {
		try {
			return base.getBlob(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnName
	 * @return result
	 */
	public Clob getClob(String columnName) {
		try {
			return base.getClob(columnName);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public boolean getBoolean(int columnIndex) {
		try {
			return base.getBoolean(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public double getDouble(int columnIndex) {
		try {
			return base.getDouble(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public float getFloat(int columnIndex) {
		try {
			return base.getFloat(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public int getInt(int columnIndex) {
		try {
			return base.getInt(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public long getLong(int columnIndex) {
		try {
			return base.getLong(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public String getString(int columnIndex) {
		try {
			return base.getString(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public Timestamp getTimestamp(int columnIndex) {
		try {
			return base.getTimestamp(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public BigDecimal getBigDecimal(int columnIndex) {
		try {
			return base.getBigDecimal(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public Object getObject(int columnIndex) {
		try {
			return base.getObject(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public InputStream getBinaryStream(int columnIndex) {
		try {
			return base.getBinaryStream(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public Reader getCharacterStream(int columnIndex) {
		try {
			return base.getCharacterStream(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public byte[] getBytes(int columnIndex) {
		try {
			return base.getBytes(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public Blob getBlob(int columnIndex) {
		try {
			return base.getBlob(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @param columnIndex
	 * @return result
	 */
	public Clob getClob(int columnIndex) {
		try {
			return base.getClob(columnIndex);
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}

	/**
	 * @return was null
	 */
	public boolean wasNull() {
		try {
			return base.wasNull();
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}
}
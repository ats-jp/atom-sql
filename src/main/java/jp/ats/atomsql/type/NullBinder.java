package jp.ats.atomsql.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jp.ats.atomsql.AtomSqlException;

class NullBinder {

	static int bind(int index, PreparedStatement statement, int sqlType) {
		try {
			statement.setNull(index, sqlType);
			return index + 1;
		} catch (SQLException e) {
			throw new AtomSqlException(e);
		}
	}
}

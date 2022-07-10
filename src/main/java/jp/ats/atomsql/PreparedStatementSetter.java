package jp.ats.atomsql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * org.springframework.jdbc.core.BatchPreparedStatementSetter
 * @author 千葉 哲嗣
 */
@FunctionalInterface
public interface PreparedStatementSetter {

	/**
	 * org.springframework.jdbc.core.BatchPreparedStatementSetter#setValues(PreparedStatement)
	 * @param ps
	 * @throws SQLException
	 */
	default void setValues(PreparedStatement ps) throws SQLException {
		setValues(ps, AtomSqlUtils.stackTrace());
	}

	/**
	 * @see #setValues(PreparedStatement)
	 * @param ps
	 * @param stackTrace
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, Optional<StackTraceElement[]> stackTrace) throws SQLException;
}

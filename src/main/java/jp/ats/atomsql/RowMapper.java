package jp.ats.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * org.springframework.jdbc.core.RowMapper
 * @author 千葉 哲嗣
 * @param <T>
 */
@FunctionalInterface
public interface RowMapper<T> {

	/**
	 * org.springframework.jdbc.core.RowMapper#mapRow(ResultSet, int)
	 * @param rs
	 * @param rowNum
	 * @return T
	 * @throws SQLException
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;
}

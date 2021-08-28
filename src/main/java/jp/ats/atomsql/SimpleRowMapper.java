package jp.ats.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 千葉 哲嗣
 * @param <T> 
 */
@FunctionalInterface
public interface SimpleRowMapper<T> {

	/**
	 * @param rs
	 * @return T
	 * @throws SQLException
	 */
	T mapRow(ResultSet rs) throws SQLException;
}

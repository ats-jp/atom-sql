package jp.ats.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 千葉 哲嗣
 * @param <R> 
 */
@FunctionalInterface
public interface Mapper<R> {

	/**
	 * @param result
	 * @return R
	 * @throws SQLException
	 */
	R map(ResultSet result) throws SQLException;
}

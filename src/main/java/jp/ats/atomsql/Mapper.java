package jp.ats.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 千葉 哲嗣
 */
@FunctionalInterface
public interface Mapper<R> {

	R map(ResultSet result) throws SQLException;
}

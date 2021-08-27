package jp.ats.furlong;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface Mapper<R> {

	R map(ResultSet result) throws SQLException;
}

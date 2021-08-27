package jp.ats.furlong;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface SimpleRowMapper<T> {

	T mapRow(ResultSet rs) throws SQLException;
}

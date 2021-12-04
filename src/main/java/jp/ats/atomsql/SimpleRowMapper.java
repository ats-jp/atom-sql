package jp.ats.atomsql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper}をより簡素化したファンクショナルインターフェイスです。
 * @see RowMapper
 * @author 千葉 哲嗣
 * @param <T> 検索結果オブジェクトの型
 */
@FunctionalInterface
public interface SimpleRowMapper<T> {

	/**
	 * @param rs {@link ResultSet}
	 * @return T 検索結果オブジェクト
	 * @throws SQLException
	 */
	T mapRow(ResultSet rs) throws SQLException;
}

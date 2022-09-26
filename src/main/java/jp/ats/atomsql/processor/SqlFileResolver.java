package jp.ats.atomsql.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import jp.ats.atomsql.annotation.SqlProxy;

/**
 * アノテーションプロセッサ実行時に、SQLファイルの実際の場所を解決する処理を表すインターフェイスです。
 * @author 千葉 哲嗣
 */
public interface SqlFileResolver {

	/**
	 * {@link SqlProxy}のメソッドに紐づくSQLファイルの内容を取得します。<br>
	 * ファイルが存在しない場合は{@link SqlFileNotFoundException}がスローされます。
	 * @param classOutput クラスファイル出力先
	 * @param packageName SQLファイルが存在するパッケージ名
	 * @param sqlFileName SQLファイル名
	 * @param options アノテーションプロセッサ実行時オプション
	 * @return byte[] SQLファイルの内容
	 * @throws IOException SQLファイル読み込み時にエラーが発生した場合
	 * @throws SqlFileNotFoundException {@Sql}が付与されていないメソッドにSQLファイルが存在しない場合
	 */
	byte[] resolve(Path classOutput, String packageName, String sqlFileName, Map<String, String> options)
		throws IOException, SqlFileNotFoundException;

	/**
	 *{@Sql}が付与されていないメソッドにSQLファイルが存在しない場合に発生する例外
	 */
	public class SqlFileNotFoundException extends Exception {

		private static final long serialVersionUID = 846870994213882546L;

		/**
		 * コンストラクタ
		 * @param message 例外メッセージ
		 */
		public SqlFileNotFoundException(String message) {
			super(message);
		}
	}
}

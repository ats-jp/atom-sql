package jp.ats.atomsql;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import jp.ats.atomsql.annotation.NoSqlLog;

/**
 * @author 千葉 哲嗣
 */
abstract class SqlLogger {

	abstract void perform(Consumer<Log> consumer);

	static final SqlLogger disabled = new SqlLogger() {

		@Override
		void perform(Consumer<Log> consumer) {
		}
	};

	static SqlLogger noSqlLogInstance(String noSqlLog) {
		return new SqlLogger() {

			@Override
			void perform(Consumer<Log> consumer) {
				AtomSql.log.info("------ @" + NoSqlLog.class.getSimpleName() + " ------ " + noSqlLog);
			}
		};
	}

	static SqlLogger instance() {
		return new SqlLoggerImpl();
	}

	private static class SqlLoggerImpl extends SqlLogger {

		@Override
		void perform(Consumer<Log> consumer) {
			if (AtomSql.configure().enableLog()) consumer.accept(AtomSql.log);
		}
	}
}

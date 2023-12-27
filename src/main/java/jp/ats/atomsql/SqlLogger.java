package jp.ats.atomsql;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Consumer;

import jp.ats.atomsql.annotation.NoSqlLog;

/**
 * @author 千葉 哲嗣
 */
abstract class SqlLogger {

	private static String noSqlLogClassName = NoSqlLog.class.getSimpleName();

	abstract void perform(Consumer<Logger> consumer);

	abstract void logElapsed(Consumer<Logger> consumer);

	static final SqlLogger disabled = new SqlLogger() {

		@Override
		void perform(Consumer<Logger> consumer) {}

		@Override
		void logElapsed(Consumer<Logger> consumer) {}
	};

	static SqlLogger noSqlLogInstance(String noSqlLogSign) {

		return new SqlLogger() {

			@Override
			void perform(Consumer<Logger> consumer) {
				AtomSql.logger.log(Level.INFO, "------ @" + noSqlLogClassName + " ------ " + noSqlLogSign);
			}

			@Override
			void logElapsed(Consumer<Logger> consumer) {
				log(consumer);
			}
		};
	}

	static SqlLogger instance() {
		return new SqlLoggerImpl();
	}

	private static class SqlLoggerImpl extends SqlLogger {

		@Override
		void perform(Consumer<Logger> consumer) {
			log(consumer);
		}

		@Override
		void logElapsed(Consumer<Logger> consumer) {
			log(consumer);
		}
	}

	private static void log(Consumer<Logger> consumer) {
		if (AtomSql.configure().enableLog()) consumer.accept(AtomSql.logger);
	}
}

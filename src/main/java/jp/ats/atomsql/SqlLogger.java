package jp.ats.atomsql;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;

/**
 * @author 千葉 哲嗣
 */
abstract class SqlLogger {

	private static final SqlLogger logger = new SqlLoggerImpl();

	private static final SqlLogger disabledLogger = new DisabledSqlLogger();

	static SqlLogger of(Configure config) {
		if (config.enableLog)
			return logger;

		return disabledLogger;

	}

	abstract void perform(Consumer<Log> consumer);

	private static class SqlLoggerImpl extends SqlLogger {

		@Override
		void perform(Consumer<Log> consumer) {
			consumer.accept(AtomSql.log);
		}
	}

	private static class DisabledSqlLogger extends SqlLogger {

		@Override
		void perform(Consumer<Log> consumer) {
		}
	}
}

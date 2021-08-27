package jp.ats.furlong;

import java.util.function.Consumer;

import org.slf4j.Logger;

abstract class SqlLogger {

	private static final SqlLogger logger = new SqlLoggerImpl();

	private static final SqlLogger disabledLogger = new DisabledSqlLogger();

	static SqlLogger of(Configure config) {
		if (config.enableLog)
			return logger;

		return disabledLogger;

	}

	abstract void perform(Consumer<Logger> consumer);

	private static class SqlLoggerImpl extends SqlLogger {

		@Override
		void perform(Consumer<Logger> consumer) {
			consumer.accept(Furlong.logger);
		}
	}

	private static class DisabledSqlLogger extends SqlLogger {

		@Override
		void perform(Consumer<Logger> consumer) {
		}
	}
}

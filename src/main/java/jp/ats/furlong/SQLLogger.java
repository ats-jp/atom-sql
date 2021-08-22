package jp.ats.furlong;

import java.util.function.Consumer;

import org.slf4j.Logger;

abstract class SQLLogger {

	private static final SQLLogger logger = new SQLLoggerImpl();

	private static final SQLLogger disabledLogger = new DisabledSQLLogger();

	static SQLLogger of(Configure config) {
		if (config.enableLog)
			return logger;

		return disabledLogger;

	}

	abstract void perform(Consumer<Logger> consumer);

	private static class SQLLoggerImpl extends SQLLogger {

		@Override
		void perform(Consumer<Logger> consumer) {
			consumer.accept(Furlong.logger);
		}
	}

	private static class DisabledSQLLogger extends SQLLogger {

		@Override
		void perform(Consumer<Logger> consumer) {
		}
	}
}

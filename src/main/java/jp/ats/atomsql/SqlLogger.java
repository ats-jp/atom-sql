package jp.ats.atomsql;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

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

	static SqlLogger instance(Configure config) {
		return new SqlLoggerImpl(config);
	}

	private static class SqlLoggerImpl extends SqlLogger {

		private final Configure config;

		SqlLoggerImpl(Configure config) {
			this.config = Objects.requireNonNull(config);
		}

		@Override
		void perform(Consumer<Log> consumer) {
			if (config.enableLog()) consumer.accept(AtomSql.log);
		}
	}
}

package jp.ats.atomsql;

import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

/**
 * @author 千葉 哲嗣
 */
class SqlLogger {

	private final Configure config;

	SqlLogger(Configure config) {
		this.config = Objects.requireNonNull(config);
	}

	void perform(Consumer<Log> consumer) {
		if (config.enableLog()) consumer.accept(AtomSql.log);

	}
}

package jp.ats.furlong;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

public class Configure {

	private final String configFileName = "furlong.properties";

	public final boolean enableLog;

	public final Pattern logStackTracePattern;

	Configure() {
		var config = new Properties();

		try {
			var input = Configure.class.getResourceAsStream(configFileName);

			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new IllegalStateException();
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "true"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));
	}
}

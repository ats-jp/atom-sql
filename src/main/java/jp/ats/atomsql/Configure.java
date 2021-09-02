package jp.ats.atomsql;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author 千葉 哲嗣
 */
public class Configure {

	/**
	 * 
	 */
	public static final Configure instance = new Configure();

	private final String configFileName = "atom-sql.properties";

	/**
	 * 
	 */
	public final boolean enableLog;

	/**
	 * 
	 */
	public final Pattern logStackTracePattern;

	private final String[] jdbcTemplateNames;

	/**
	 * @return jdbcTemplateNames
	 */
	public String[] jdbcTemplateNames() {
		return jdbcTemplateNames.clone();
	}

	private Configure() {
		var config = new Properties();

		try {
			var input = Configure.class.getClassLoader().getResourceAsStream(configFileName);

			if (input != null)
				config.load(input);
		} catch (IOException e) {
			throw new IllegalStateException();
		}

		enableLog = Boolean.valueOf(config.getProperty("enable-log", "true"));

		logStackTracePattern = Pattern.compile(config.getProperty("log-stacktrace-pattern", ".+"));

		jdbcTemplateNames = config.getProperty("jdbc-template-names", "").split(" *, *");
	}
}

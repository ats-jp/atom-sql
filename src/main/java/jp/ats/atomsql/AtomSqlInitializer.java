package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author 千葉 哲嗣
 */
public class AtomSqlInitializer {

	/**
	 * @throws IOException
	 */
	public void start() throws IOException {
		List<Class<?>> classes;

		try {
			classes = loadProxyClasses();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (var context = new AnnotationConfigApplicationContext()) {
			var names = context.getBeanNamesForType(JdbcTemplate.class);

			Arrays.stream(names).forEach(name -> {
				var jdbcTemplate = context.getBean(name, JdbcTemplate.class);

				var atomSql = new AtomSql(jdbcTemplate);

				context.registerBean(name, AtomSql.class, () -> atomSql, bd -> {
					bd.setScope(BeanDefinition.SCOPE_SINGLETON);
				});

				classes.forEach(c -> {
					@SuppressWarnings("unchecked")
					var casted = (Class<Object>) c;
					context.registerBean(name, casted, () -> {
						var instance = atomSql.of(c);
						return instance;
					}, bd -> {
						bd.setScope(BeanDefinition.SCOPE_SINGLETON);
					});
				});
			});
		}
	}

	private List<Class<?>> loadProxyClasses() throws IOException {
		try (var proxyList = getClass().getClassLoader().getResourceAsStream(Constants.PROXY_LIST)) {
			return Arrays.stream(new String(Utils.readBytes(proxyList), StandardCharsets.UTF_8).split("\\s+")).map(l -> {
				try {
					return Class.forName(l);
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}).collect(Collectors.toList());
		}
	}
}

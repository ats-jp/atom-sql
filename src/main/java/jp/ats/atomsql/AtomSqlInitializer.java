package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author 千葉 哲嗣
 */
@Configuration
public class AtomSqlInitializer {

	/**
	 * @return SmartInitializingSingleton
	 */
	@Bean
	public SmartInitializingSingleton start() {
		return () -> {
			List<Class<?>> classes;

			try {
				classes = loadProxyClasses();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			try (var context = new AnnotationConfigApplicationContext()) {
				var names = Configure.instance.jdbcTemplateNames();

				Arrays.stream(names).forEach(name -> {
					context.registerBean(name, AtomSql.class, () -> {
						return new AtomSql(context.getBean(name, JdbcTemplate.class));
					}, bd -> {
						bd.setScope(BeanDefinition.SCOPE_SINGLETON);
						bd.setLazyInit(true);
					});

					classes.forEach(c -> {
						@SuppressWarnings("unchecked")
						var casted = (Class<Object>) c;
						context.registerBean(name, casted, () -> {
							var atomSql = new AtomSql(context.getBean(name, JdbcTemplate.class));
							return atomSql.of(c);
						}, bd -> {
							bd.setScope(BeanDefinition.SCOPE_SINGLETON);
							bd.setLazyInit(true);
						});
					});
				});

				context.refresh();
			}
		};
	}

	private static List<Class<?>> loadProxyClasses() throws IOException {
		try (var proxyList = AtomSqlInitializer.class.getClassLoader().getResourceAsStream(Constants.PROXY_LIST)) {
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

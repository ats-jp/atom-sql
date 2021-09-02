package jp.ats.atomsql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author 千葉 哲嗣
 */
@Configuration
public class AtomSqlConfiguration {

	@Autowired
	AtomSqlConfiguration(Map<String, JdbcTemplate> map) throws IOException {
		List<Class<?>> classes;
		try (var proxyList = getClass().getClassLoader().getResourceAsStream(Constants.PROXY_LIST)) {
			classes = Arrays.stream(new String(Utils.readBytes(proxyList), StandardCharsets.UTF_8).split("\\s+")).map(l -> {
				try {
					return Class.forName(l);
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}).collect(Collectors.toList());
		}

		try (var context = new AnnotationConfigApplicationContext()) {
			map.forEach((qualifier, jdbcTemplate) -> {
				var atomSql = new AtomSql(jdbcTemplate);

				context.registerBean(qualifier, AtomSql.class, () -> atomSql, bd -> {
					bd.setScope(BeanDefinition.SCOPE_SINGLETON);
				});

				classes.forEach(c -> {
					@SuppressWarnings("unchecked")
					var casted = (Class<Object>) c;
					context.registerBean(qualifier, casted, () -> {
						var instance = atomSql.of(c);
						return instance;
					}, bd -> {
						bd.setScope(BeanDefinition.SCOPE_SINGLETON);
					});
				});
			});
		}
	}
}

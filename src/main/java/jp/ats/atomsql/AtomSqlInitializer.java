package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import jp.ats.atomsql.annotation.SqlProxy;

/**
 * Atom SQLをSpringで使用できるように初期化するクラスです。<br>
 * {@link SqlProxy}が付与されたクラスを{@link Autowired}可能にします。<br>
 * @see SpringApplication#addInitializers(ApplicationContextInitializer...)
 * @author 千葉 哲嗣
 */
public class AtomSqlInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		List<Class<?>> classes;
		try {
			classes = loadProxyClasses();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		BeanDefinitionCustomizer customizer = bd -> {
			bd.setScope(BeanDefinition.SCOPE_SINGLETON);
			bd.setLazyInit(true);
			bd.setAutowireCandidate(true);
			bd.setPrimary(true);
		};

		context.registerBean(AtomSql.class.getName(), AtomSql.class, () -> new AtomSql(convert(context)), customizer);

		classes.forEach(c -> {
			@SuppressWarnings("unchecked")
			var casted = (Class<Object>) c;

			context.registerBean(c.getName(), casted, () -> new AtomSql(convert(context)).of(casted), customizer);
		});
	}

	private static List<Class<?>> loadProxyClasses() throws IOException {
		try (var proxyList = AtomSqlInitializer.class.getClassLoader().getResourceAsStream(Constants.PROXY_LIST)) {
			return Arrays.stream(new String(Utils.readBytes(proxyList), Constants.CHARSET).split("\\s+")).map(l -> {
				try {
					return Class.forName(l);
				} catch (ClassNotFoundException e) {
					//コンパイラの動作によっては削除されたクラスがまだ残っているかもしれないのでスキップ
					return null;
				}
			}).filter(c -> c != null).collect(Collectors.toList());
		}
	}

	private static Executors convert(GenericApplicationContext context) {
		var map = context.getBeansOfType(JdbcTemplate.class);
		var primary = context.getBean(JdbcTemplate.class);

		var entries = map.entrySet().stream().map(e -> {
			var jdbcTemplate = e.getValue();
			return new Executors.Entry(e.getKey(), new JdbcTemplateExecutor(jdbcTemplate), jdbcTemplate == primary);
		}).toArray(Executors.Entry[]::new);

		return new Executors(entries);
	}
}

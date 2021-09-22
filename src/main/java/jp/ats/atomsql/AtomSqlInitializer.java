package jp.ats.atomsql;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * {@link JdbcTemplate}が{@link Qualifier}によって複数設定されている環境のために、{SqlProxy}にも同じ識別子が使用可能になるように設定可能です。
 * @see SpringApplication#addInitializers(ApplicationContextInitializer...)
 * @author 千葉 哲嗣
 */
public class AtomSqlInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	private final String name;

	private final boolean primary;

	/**
	 * {@link JdbcTemplate}を単体で使用する場合に使用するコンストラクタです。
	 */
	public AtomSqlInitializer() {
		this.name = null;
		primary = true;
	}

	/**
	 * {@link JdbcTemplate}を複数で使用する場合に使用するコンストラクタです。<br>
	 * primaryは、{@AtomSqlInitializer}を複数設定する場合、一つのインスタンスでのみtrueにしてください。
	 * @param name {@link JdbcTemplate}用識別子（{@link Qualifier}）
	 * @param primary {@link Qualifier}を指定しない場合に優先するか
	 */
	public AtomSqlInitializer(String name, boolean primary) {
		this.name = Objects.requireNonNull(name);
		this.primary = primary;
	}

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
			bd.setPrimary(primary);
		};

		context.registerBean(name, AtomSql.class, () -> {
			if (name == null) {
				return new AtomSql(new JdbcTemplateExecutor(context.getBean(JdbcTemplate.class)));
			}

			return new AtomSql(new JdbcTemplateExecutor(context.getBean(name, JdbcTemplate.class)));
		}, customizer);

		classes.forEach(c -> {
			@SuppressWarnings("unchecked")
			var casted = (Class<Object>) c;
			context.registerBean(name, casted, () -> {
				if (name == null) {
					var atomSql = new AtomSql(new JdbcTemplateExecutor(context.getBean(JdbcTemplate.class)));
					return atomSql.of(c);
				}

				var atomSql = new AtomSql(new JdbcTemplateExecutor(context.getBean(name, JdbcTemplate.class)));
				return atomSql.of(c);
			}, customizer);
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
}

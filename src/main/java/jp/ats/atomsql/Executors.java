package jp.ats.atomsql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link Executor}を複数管理するためのマップクラスです。
 * @author 千葉 哲嗣
 */
public class Executors {

	private final Entry primary;

	private final Map<String, Entry> map;

	/**
	 * 複数の{@link Executor}を設定してインスタンスを生成します。
	 * @param entries
	 */
	public Executors(Entry... entries) {
		if (entries.length == 0) throw new IllegalArgumentException("Empty entries");

		map = new LinkedHashMap<>();
		Entry primary = null;
		for (var entry : entries) {
			if (primary != null) throw new IllegalArgumentException("Primary entry is duplicate");
			if (entry.primary) primary = entry;

			map.put(entry.name, entry);
		}

		if (primary == null) throw new IllegalArgumentException("Primary entry not found");

		this.primary = primary;
	}

	/**
	 * 単体の{@link Executor}を設定してインスタンスを生成します。
	 * @param primaryExecutor
	 */
	public Executors(Executor primaryExecutor) {
		this(new Entry(primaryExecutor));
	}

	/**
	 * {@link JdbcTemplate}のBean名をもとに対応する{@link Entry}を返します。
	 * @param name Bean名 nullの場合プライマリ{@link Executor}が返却される
	 * @return {@link Entry}
	 */
	public Entry get(String name) {
		return map.get(name);
	}

	/**
	 * プライマリ{@link Executor}を返します。
	 * @return {@link Entry}
	 */
	public Entry get() {
		return primary;
	}

	/**
	 * {@link Executors}用要素
	 */
	public static class Entry {

		final String name;

		final Executor executor;

		private final boolean primary;

		/**
		 * @param name {@link JdbcTemplate} Bean名
		 * @param executor {@link Executor}
		 * @param primary プライマリBeanかどうか
		 */
		public Entry(String name, Executor executor, boolean primary) {
			this.name = Objects.requireNonNull(name);
			this.executor = Objects.requireNonNull(executor);
			this.primary = primary;
		}

		/**
		 * @param executor {@link Executor}
		 */
		public Entry(Executor executor) {
			this.name = null;
			this.executor = Objects.requireNonNull(executor);
			this.primary = true;
		}
	}
}

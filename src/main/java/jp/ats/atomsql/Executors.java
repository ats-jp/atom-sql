package jp.ats.atomsql;

import java.util.LinkedHashMap;
import java.util.Map;

import jp.ats.atomsql.annotation.Qualifier;

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
			if (entry.primary()) {
				if (primary != null) throw new IllegalArgumentException("Primary entry is duplicate");
				primary = entry;
			}

			map.put(entry.name(), entry);
		}

		if (primary == null) throw new IllegalArgumentException("Primary entry not found");

		this.primary = primary;
	}

	/**
	 * 単体の{@link Executor}を設定してインスタンスを生成します。
	 * @param primaryExecutor
	 */
	public Executors(Executor primaryExecutor) {
		this(new Entry(null, primaryExecutor, true));
	}

	/**
	 * {@link Qualifier}の値をもとに対応する{@link Entry}を返します。
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
	 * @param name {@link Qualifier}名
	 * @param executor {@link Executor}
	 * @param primary プライマリBeanかどうか
	 */
	public static record Entry(String name, Executor executor, boolean primary) {
	}
}

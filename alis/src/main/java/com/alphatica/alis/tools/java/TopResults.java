package com.alphatica.alis.tools.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class TopResults<T> {
	private final int limit;
	private final Comparator<T> order;
	private final Function<T, ?> uniqueKey;
	private final List<T> results = new ArrayList<>();

	public TopResults(int limit, Comparator<T> order) {
		this(limit, order, null);
	}

	public TopResults(int limit, Comparator<T> order, Function<T, ?> uniqueKey) {
		this.limit = limit;
		this.order = order;
		this.uniqueKey = uniqueKey;
	}

	/**
	 * @return true when the list of top results has changed
	 */
	public synchronized boolean add(T result) {
		if (isDuplicate(result)) {
			return false;
		}
		results.add(result);
		results.sort(order);
		if (results.size() > limit) {
			T removed = results.removeLast();
			return !removed.equals(result);
		}
		return true;
	}

	public synchronized List<T> list() {
		return List.copyOf(results);
	}

	public synchronized void clear() {
		results.clear();
	}

	private boolean isDuplicate(T result) {
		if (uniqueKey == null) {
			return false;
		}
		Object key = uniqueKey.apply(result);
		return results.stream().anyMatch(existing -> uniqueKey.apply(existing).equals(key));
	}
}

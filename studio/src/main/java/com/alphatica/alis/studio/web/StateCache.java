package com.alphatica.alis.studio.web;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Value computed from application state once per change instead of once per request.
 * The state it depends on is given as a key: the value is computed again when the key is no longer equal.
 */
public final class StateCache<T> {
	private final Callable<T> loader;
	private final Supplier<?> key;
	private Object loadedKey;
	private T value;

	/**
	 * @param key the state the value is computed from, e.g. the loaded market data
	 */
	public StateCache(Callable<T> loader, Supplier<?> key) {
		this.loader = loader;
		this.key = key;
	}

	public synchronized T get() throws Exception {
		Object currentKey = key.get();
		if (value == null || !Objects.equals(currentKey, loadedKey)) {
			value = loader.call();
			loadedKey = currentKey;
		}
		return value;
	}
}

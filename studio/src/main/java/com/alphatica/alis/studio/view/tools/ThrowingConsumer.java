package com.alphatica.alis.studio.view.tools;

@FunctionalInterface
public interface ThrowingConsumer<T> {

	@SuppressWarnings("java:S112")
	void accept(T t) throws Exception;
}

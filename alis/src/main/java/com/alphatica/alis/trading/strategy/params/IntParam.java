package com.alphatica.alis.trading.strategy.params;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntParam implements Param {
	private final String name;
	private final Consumer<Integer> setter;
	private final Supplier<Integer> getter;
	private final Supplier<Integer> generator;

	public IntParam(String name, Supplier<Integer> generator, Consumer<Integer> setter, Supplier<Integer> getter) {
		this.name = name;
		this.setter = setter;
		this.generator = generator;
		this.getter = getter;
	}

	@Override
	public void setFromSupplier() {
		setter.accept(generator.get());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object get() {
		return getter.get();
	}

	@Override
	public void set(Object o) {
		setter.accept((Integer) o);
	}
}

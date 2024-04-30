package com.alphatica.alis.trading.strategy.params;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DoubleParam implements Param {
	private final String name;
	private final Supplier<Double> generator;
	private final Consumer<Double> setter;
	private final Supplier<Double> getter;

	public DoubleParam(String name, Supplier<Double> generator, Consumer<Double> setter, Supplier<Double> getter) {
		this.name = name;
		this.generator = generator;
		this.setter = setter;
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
		setter.accept((Double)o);
	}
}

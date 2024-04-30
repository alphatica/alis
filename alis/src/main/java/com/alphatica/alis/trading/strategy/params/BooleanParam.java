package com.alphatica.alis.trading.strategy.params;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanParam implements Param {
	private final String name;
	private final Consumer<Boolean> setter;
	private final Supplier<Boolean> getter;

	public BooleanParam(String name, Consumer<Boolean> setter, Supplier<Boolean> getter) {
		this.name = name;
		this.setter = setter;
		this.getter = getter;
	}

	@Override
	public void setFromSupplier() {
		setter.accept(ThreadLocalRandom.current().nextBoolean());
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
		setter.accept((Boolean) o);
	}
}

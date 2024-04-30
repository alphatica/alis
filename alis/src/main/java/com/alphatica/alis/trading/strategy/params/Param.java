package com.alphatica.alis.trading.strategy.params;

import java.util.List;

public interface Param {

	void setFromSupplier();

	String getName();

	Object get();

	void set(Object o);

	static void copyParams(List<Param> source, List<Param> target) {
		if (source.size() != target.size()) {
			throw new IllegalArgumentException("Source and target must have the same size.");
		}
		for(int i = 0; i < source.size(); i++) {
			Param s = source.get(i);
			Param t = target.get(i);
			if (!s.getName().equals(t.getName())) {
				throw new IllegalArgumentException("Source and target must have the same name: " + s.getName() + " vs. " + t.getName());
			}
			t.set(s.get());
		}
	}

	static String show(Param param) {
		if (param instanceof DoubleParam) {
			double v = (double) param.get();
			return String.format("%s = %.3f;", param.getName(), v);
		}
		if (param instanceof IntParam) {
			int v = ((int) param.get());
			return String.format("%s = %d;", param.getName(), v);
		}
		if (param instanceof BooleanParam) {
			boolean v = (boolean) param.get();
			return String.format("%s = %b;", param.getName(), v);
		}
		throw new IllegalArgumentException("Unsupported param type: " + param.getClass().getName());
	}
}

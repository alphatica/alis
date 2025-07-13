package com.alphatica.alis.trading.optimizer;


import java.util.concurrent.ThreadLocalRandom;

public class ParamSteps {
	private final Object[] values;

	public ParamSteps(int start, int step, int end) {
		int size = (end - start) / step + 1;
		values = new Object[size];
		int s = start;
		int i = 0;
		while (s <= end) {
			values[i++] = s;
			s += step;
		}
	}

	public ParamSteps() {
		values = new Object[2];
		values[0] = true;
		values[1] = false;
	}

	public ParamSteps(double start, double step, double end) {
		int size = (int) Math.floor((end - start) / step + 1);
		values = new Object[size];
		double s = start;
		for (int i = 0; i < size; i++) {
			values[i] = s;
			s += step;
		}
	}

	public Object getRandom() {
		return values[ThreadLocalRandom.current().nextInt(values.length)];
	}

	public int size() {
		return values.length;
	}

	public Object[] values() {
		return values;
	}
}

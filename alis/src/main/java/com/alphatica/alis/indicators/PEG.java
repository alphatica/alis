package com.alphatica.alis.indicators;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.FloatArraySlice;

import java.util.ArrayList;
import java.util.List;

public class PEG extends Indicator {

	private final int[] windows;

	/**
	 * Use
	 * new PEG(0, 250, 500, 750);
	 * to get last 3 years on daily data.
	 *
	 * @param windows
	 */
	public PEG(int... windows) {
		this.windows = windows;
	}

	@Override
	public float calculate(TimeMarketData data) {
		FloatArraySlice pe = data.getLayer(Layer.PE);
		FloatArraySlice mv = data.getLayer(Layer.MV);
		if (pe.size() < offset + windows[windows.length - 1] || mv.size() < offset + windows[windows.length - 1]) {
			return Float.NaN;
		}
		List<Double> profits = new ArrayList<>(windows.length);
		for (int window : windows) {
			double profit = mv.get(window + offset) / pe.get(window + offset);
			if (profit < 0) {
				return Float.NaN;
			}
			profits.add(profit);
		}
		double changes = 0;
		int changesCount = 0;
		double lastProfit = profits.removeFirst();
		while (!profits.isEmpty()) {
			float change = (float)(100 * (lastProfit / profits.getFirst() - 1));
			changes += change;
			changesCount++;
			lastProfit = profits.removeFirst();
		}
		if (changesCount > 0) {
			float average = (float)changes / changesCount;
			return pe.get(offset) / average;
		} else {
			return Float.NaN;
		}
	}
}

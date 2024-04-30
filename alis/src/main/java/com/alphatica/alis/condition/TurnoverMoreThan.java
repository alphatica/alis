package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.DoubleArraySlice;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;

public record TurnoverMoreThan(double minTurnover, int bars) implements Condition {
	@Override
	public boolean matches(TimeMarketData marketData, TimeMarketDataSet all) {
		double turnoverSoFar = 0.0;
		DoubleArraySlice turnoverData = marketData.getLayer(TURNOVER);
		for (int i = 0; i < bars && i < turnoverData.size(); i++) {
			turnoverSoFar += turnoverData.get(i);
			if (turnoverSoFar >= minTurnover) {
				return true;
			}
		}
		return false;
	}
}

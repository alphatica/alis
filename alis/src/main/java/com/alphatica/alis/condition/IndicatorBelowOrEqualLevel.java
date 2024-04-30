package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.Indicator;

public class IndicatorBelowOrEqualLevel implements Condition {
	private final Indicator indicator;
	private final double value;

	public IndicatorBelowOrEqualLevel(Indicator indicator, double value) {
		this.indicator = indicator;
		this.value = value;
	}

	@Override
	public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
		return indicator.calculate(market) <= value;
	}
}

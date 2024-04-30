package com.alphatica.alis.indicators.candlesticks;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.HighestHigh;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.indicators.trend.Sma;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.OPEN;


public class BearishEngulfment implements Condition {

	private static final int LENGTH = 10;
	private final Indicator sma = new Sma(LENGTH);
	private final Condition highestHigh = new HighestHigh(LENGTH);


	@Override
	public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
		if (market.getLayer(CLOSE).size() < LENGTH) {
			return false;
		}
		if (sma.calculate(market) > market.getData(CLOSE, 1)) {
			return false;
		}
		if (!highestHigh.matches(market, allMarkets)) {
			return false;
		}
		//	return true here to avoid formation check
		double todayOpen = market.getData(OPEN, 0);
		double todayClose = market.getData(CLOSE, 0);
		double yesterdayClose = market.getData(CLOSE, 1);
		double yesterdayOpen = market.getData(OPEN, 1);
		double yesterdayLow = market.getData(LOW, 1);
		if (todayOpen < yesterdayClose) {
			return false;
		}
		if (todayOpen < yesterdayOpen) {
			return false;
		}
		if (todayClose > todayOpen) {
			return false;
		}
		return todayClose < yesterdayLow;
	}
}

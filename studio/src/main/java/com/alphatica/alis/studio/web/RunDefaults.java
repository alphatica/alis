package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;

import java.util.List;

/**
 * Defaults of the backtest and optimization forms: the last 10 years (2500 sessions) of market data, empty times
 * without data.
 */
record RunDefaults(String startTime, String endTime, String commissionRate, String initialCapital) {
	private static final int DEFAULT_BARS = 250 * 10;

	static RunDefaults current() {
		MarketData marketData = AppState.getMarketData();
		if (marketData == null || marketData.getTimes().isEmpty()) {
			return new RunDefaults("", "", "0.01", "100000");
		}
		List<Time> times = marketData.getTimes();
		Time start = times.size() > DEFAULT_BARS ? times.get(times.size() - DEFAULT_BARS) : times.getFirst();
		return new RunDefaults(start.toString(), times.getLast().toString(), "0.01", "100000");
	}
}

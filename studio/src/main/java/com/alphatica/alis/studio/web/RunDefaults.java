package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;

import java.time.LocalDate;
import java.util.List;

/**
 * Defaults of the backtest, optimization and parameter selection check forms: from the first session of the year 10 years
 * before the year of the last session (the whole data if it starts later) to the last session; empty times without
 * data.
 */
record RunDefaults(String startTime, String endTime, String commissionRate, String initialCapital) {
	private static final int DEFAULT_YEARS = 10;

	static RunDefaults current() {
		return of(AppState.getMarketData());
	}

	static RunDefaults of(MarketData marketData) {
		if (marketData == null || marketData.getTimes().isEmpty()) {
			return new RunDefaults("", "", "0.01", "100000");
		}
		List<Time> times = marketData.getTimes();
		int startYear = times.getLast().toLocalDate().getYear() - DEFAULT_YEARS;
		Time newYear = Time.of(LocalDate.of(startYear, 1, 1));
		Time start = times.stream().filter(time -> !time.isBefore(newYear)).findFirst().orElseThrow();
		return new RunDefaults(start.toString(), times.getLast().toString(), "0.01", "100000");
	}
}

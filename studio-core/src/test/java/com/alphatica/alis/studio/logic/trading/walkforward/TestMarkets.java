package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.loader.stooq.StooqMarket;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntToDoubleFunction;

/** Stocks quoted on every weekday from 2018 to 2021, each with its own daily growth factor in every year. */
final class TestMarkets {
	private static final LocalDate FIRST = LocalDate.of(2018, 1, 1);
	private static final LocalDate LAST = LocalDate.of(2021, 12, 31);

	private TestMarkets() {
	}

	/**
	 * @param dailyGrowth by market name: the factor the price grows by every session of a year
	 */
	static StandardMarketData of(Map<String, IntToDoubleFunction> dailyGrowth) {
		List<Time> times = new ArrayList<>();
		for (LocalDate day = FIRST; !day.isAfter(LAST); day = day.plusDays(1)) {
			if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
				times.add(Time.of(day));
			}
		}
		Map<MarketName, Market> markets = new HashMap<>();
		dailyGrowth.forEach((name, growth) -> {
			float[] newestFirst = new float[times.size()];
			double price = 100;
			for (int i = 0; i < times.size(); i++) {
				price *= growth.applyAsDouble(times.get(i).time() / 10000);
				newestFirst[times.size() - 1 - i] = (float) price;
			}
			TreeMap<Time, List<FloatArraySlice>> ranges = new TreeMap<>();
			for (int i = 0; i < times.size(); i++) {
				FloatArraySlice prices = new FloatArraySlice(newestFirst, times.size() - 1 - i);
				ranges.put(times.get(i), Collections.nCopies(6, prices));
			}
			MarketName marketName = new MarketName(name);
			markets.put(marketName, new StooqMarket(ranges, marketName, MarketType.STOCK));
		});
		StandardMarketData data = new StandardMarketData();
		data.addMarkets(markets);
		return data;
	}
}

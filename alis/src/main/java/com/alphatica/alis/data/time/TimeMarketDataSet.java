package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.alphatica.alis.data.market.MarketFilters.ALL;

public class TimeMarketDataSet {

	private final Map<MarketName, TimeMarketData> set;
	private final Time time;

	public TimeMarketDataSet(Map<MarketName, TimeMarketData> set, Time time) {
		this.set = set;
		this.time = time;
	}

	public static TimeMarketDataSet build(Time time, MarketData marketData) {
		List<Market> markets = marketData.listMarkets(ALL);
		Map<MarketName, TimeMarketData> result = HashMap.newHashMap(markets.size());
		for (Market market : markets) {
			TimeMarketData timeData = market.getAtOrPrevious(time);
			if (timeData != null) {
				result.put(market.getName(), timeData);
			}
		}
		return new TimeMarketDataSet(result, time);
	}

	public TimeMarketData get(MarketName name) {
		return set.get(name);
	}

	public Time getTime() {
		return time;
	}

	public List<TimeMarketData> listUpToDateMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(filter).filter(m -> m.getTime().equals(time)).toList();
	}

	public List<TimeMarketData> listAllKnownMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(filter).toList();
	}
}

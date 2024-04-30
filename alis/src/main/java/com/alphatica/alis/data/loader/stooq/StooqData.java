package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class StooqData implements MarketData {
	private final Map<MarketName, Market> markets;
	private List<Time> times;

	public StooqData() {
		markets = new HashMap<>();
		times = new ArrayList<>();
	}

	public void addMarkets(Map<MarketName, Market> newMarkets) {
		markets.putAll(newMarkets);
		Set<Time> timeHashSet = new HashSet<>();
		for (Market stock : markets.values()) {
			timeHashSet.addAll(stock.getTimes());
		}
		this.times = timeHashSet.stream().sorted().toList();
	}

	@Override
	public List<Time> getTimes() {
		return times;
	}

	@Override
	public Market getMarket(MarketName marketName) {
		return markets.get(marketName);
	}

	@Override
	public List<Market> listMarkets(Predicate<Market> filter) {
		return markets.values().stream().filter(filter).map(Market.class::cast).toList();
	}


}

package com.alphatica.alis.tools.data;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestData implements MarketData {

	private final Map<MarketName, Market> markets = new HashMap<>();
	private final MarketName marketName = new MarketName("test_market");
	private final MarketType marketType = MarketType.STOCK;

	public TestData() {
		TreeMap<Time, TimeMarketData> data = new TreeMap<>();
		final int COUNT = 1024;
		float[] allPrices = new float[COUNT];
		for (int i = 0; i < COUNT; i++) {
			allPrices[i] = COUNT - i;
		}
		for (int i = 0; i < COUNT; i++) {
			putData(data, new Time(i + 1), new FloatArraySlice(allPrices, allPrices.length - i - 1));
		}
		Market market = new TestMarket(marketName, data);
		markets.put(market.getName(), market);
	}

	@Override
	public List<Time> getTimes() {
		Set<Time> set = markets.values().stream().map(Market::getTimes).flatMap(Collection::stream).collect(Collectors.toSet());
		return set.stream().sorted().toList();
	}

	@Override
	public Market getMarket(MarketName marketName) {
		return markets.get(marketName);
	}

	@Override
	public List<Market> listMarkets(Predicate<Market> filter) {
		return markets.values().stream().filter(filter).toList();
	}

	private void putData(TreeMap<Time, TimeMarketData> data, Time time, FloatArraySlice floatArraySlice) {
		TimeMarketData timeMarketData = new TimeMarketData(marketName, marketType, time, List.of(floatArraySlice, floatArraySlice,
				floatArraySlice, floatArraySlice));
		data.put(time, timeMarketData);
	}
}
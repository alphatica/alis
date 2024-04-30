package com.alphatica.alis.tools.data.loader;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArrayRange;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TestData implements MarketData {

    private final Map<MarketName, Market> markets;
    private final MarketName marketName = new MarketName("test_market");
    private final MarketType marketType = MarketType.STOCK;

    public TestData() {
        markets = new HashMap<>();
        TreeMap<Time, TimeMarketData> data = new TreeMap<>();
        double[] allPrices = {1.0, 2.0, 3.0, 4.0, 5.0};
        putData(data, new Time(0), new DoubleArrayRange(allPrices, 3, 4));
        putData(data, new Time(1), new DoubleArrayRange(allPrices, 2, 4));
        putData(data, new Time(2), new DoubleArrayRange(allPrices, 1, 4));
        putData(data, new Time(3), new DoubleArrayRange(allPrices, 0, 4));
        Market market = new TestMarket(marketName, data);
        markets.put(market.getName(), market);
    }

    private void putData(TreeMap<Time, TimeMarketData> data, Time time, DoubleArrayRange doubleArrayRange) {
        TimeMarketData timeMarketData = new TimeMarketData(marketName, marketType, time, List.of(doubleArrayRange, doubleArrayRange, doubleArrayRange, doubleArrayRange));
        data.put(time, timeMarketData);
    }

    @Override
    public List<Time> getTimes() {
        Set<Time> set = markets.values().stream().map(Market::getTimes).flatMap(Collection::stream).collect(Collectors.toSet());
        return set.stream().sorted().toList();
    }

    @Override
    public Optional<Market> getMarket(MarketName marketName) {
        return Optional.empty();
    }

    @Override
    public List<Market> listMarkets(Predicate<Market> filter) {
        return markets.values().stream().filter(filter).toList();
    }
}
package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArrayRange;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Optional.ofNullable;

public class StooqMarket implements Market {
    private final TreeMap<Time, List<DoubleArrayRange>> ranges;
    private final MarketName name;
    private final MarketType marketType;

    @SuppressWarnings("java:S1319")
    public StooqMarket(TreeMap<Time, List<DoubleArrayRange>> ranges, MarketName name, MarketType marketType) {
        this.ranges = ranges;
        this.name = name;
        this.marketType = marketType;
    }

    @Override
    public List<Time> getTimes() {
        return ranges.keySet().stream().sorted().toList();
    }

    @Override
    public MarketName getName() {
        return name;
    }

    @Override
    public MarketType getType() {
        return marketType;
    }

    @Override
    public Optional<TimeMarketData> getAtOrPrevious(Time time) {
        return ofNullable(ranges.floorEntry(time)).map(entry -> new TimeMarketData(name, marketType, entry.getKey(), entry.getValue()));
    }

    @Override
    public Optional<TimeMarketData> getAtOrNext(Time time) {
        return ofNullable(ranges.ceilingEntry(time)).map(entry -> new TimeMarketData(name, marketType, entry.getKey(), entry.getValue()));
    }

    @Override
    public Optional<TimeMarketData> getAt(Time time) {
        return ofNullable(ranges.get(time)).map(entry -> new TimeMarketData(name, marketType, time, entry));
    }
}

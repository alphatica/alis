package com.alphatica.alis.tools.data.loader;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Optional.ofNullable;

public record TestMarket(MarketName name, TreeMap<Time, TimeMarketData> data) implements Market {

    @Override
    public MarketName getName() {
        return name;
    }

    @Override
    public MarketType getType() {
        return MarketType.STOCK;
    }

    @Override
    public List<Time> getTimes() {
        return data.keySet().stream().sorted().toList();
    }

    @Override
    public Optional<TimeMarketData> getAtOrPrevious(Time time) {
        return ofNullable(data.floorEntry(time).getValue());
    }

    @Override
    public Optional<TimeMarketData> getAtOrNext(Time time) {
        return ofNullable(data.ceilingEntry(time).getValue());
    }

    @Override
    public Optional<TimeMarketData> getAt(Time time) {
        return ofNullable(data.get(time));
    }
}

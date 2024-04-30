package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;
import java.util.Optional;

public interface Market {
    MarketName getName();

    MarketType getType();

    List<Time> getTimes();

    Optional<TimeMarketData> getAtOrPrevious(Time time);

    Optional<TimeMarketData> getAtOrNext(Time time);

    Optional<TimeMarketData> getAt(Time time);
}

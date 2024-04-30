package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface MarketData {
    List<Time> getTimes();

    Optional<Market> getMarket(MarketName marketName);

    List<Market> listMarkets(Predicate<Market> filter);

    default MarketData fromSingle(Market market) {
        return new MarketData() {

            @Override
            public List<Time> getTimes() {
                return market.getTimes();
            }

            @Override
            public Optional<Market> getMarket(MarketName marketName) {
                if (marketName.equals(market.getName())) {
                    return Optional.of(market);
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public List<Market> listMarkets(Predicate<Market> filter) {
                return List.of(market);
            }
        };
    }

}

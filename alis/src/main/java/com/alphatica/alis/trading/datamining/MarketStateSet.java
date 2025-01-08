package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketName;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MarketStateSet {
    private final Map<MarketName, MarketState> map = new HashMap<>();

    public MarketState get(MarketName marketName, Supplier<MarketState> defaultSupplier) {
        MarketState state = map.get(marketName);
        if (state == null) {
            MarketState n = defaultSupplier.get();
            map.put(marketName, n);
            return n;
        } else {
            return state;
        }
    }

    public void delete(MarketName market) {
        map.remove(market);
    }
}

package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public interface Condition {
    static Condition all(Condition... conditions) {
        return (market, allData) -> {
            for (Condition condition : conditions) {
                if (!condition.matches(market, allData)) {
                    return false;
                }
            }
            return true;
        };
    }

    boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets);
}

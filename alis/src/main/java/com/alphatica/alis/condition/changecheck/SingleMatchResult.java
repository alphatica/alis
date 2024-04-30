package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;

public record SingleMatchResult(Time time, Market market, Time openTime, double openPrice, Time closeTime,
                                double closePrice, double change) {
    @Override
    public String toString() {
        return "startTime: " + time + " market: " + market.getName() + " openTime: " + openTime + " openPrice: " + openPrice + " closeTime: " + closeTime + " closePrice: " + closePrice + " change: " + change;
    }
}

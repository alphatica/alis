package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.Liquidity;

public record LiquidityMoreThan(double minTurnover, int bars) implements Condition {
    @Override
    public boolean matches(TimeMarketData marketData, TimeMarketDataSet all) {
        return Liquidity.isLiquid(marketData, minTurnover, bars);
    }
}

package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;

public class MinMaxTradeSignal extends TradeSignal {
    private final MinMax minMax = new MinMax(250);

    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (minMax.calculate(marketData) > 0) {
            return 1.0f;
        } else {
            return Float.NaN;
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return minMax.calculate(marketData) < 0;
    }

}

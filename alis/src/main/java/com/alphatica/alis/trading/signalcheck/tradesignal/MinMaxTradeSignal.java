package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;

public class MinMaxTradeSignal implements TradeSignal {
    private final MinMax minMax = new MinMax(250);

    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return minMax.calculate(marketData) > 0 ? 1.0f : Float.NaN;
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return minMax.calculate(marketData) < 0;
    }

    @Override
    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {

    }
}

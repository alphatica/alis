package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;

public class SmaCrossTradeSignal implements TradeSignal {
    private final Sma smaShort = new Sma(50);
    private final Sma smaLong = new Sma(200);
    
    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return smaShort.calculate(marketData) > smaLong.calculate(marketData) ? 1 : 0;
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return smaShort.calculate(marketData) < smaLong.calculate(marketData);
    }

    @Override
    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {

    }
}

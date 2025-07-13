package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public interface TradeSignal {

    float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

}

package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.optimizer.Optimizable;

public abstract class TradeSignal implements Optimizable {

    public abstract float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    public abstract boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {}

    public void paramsChanged() {}
}

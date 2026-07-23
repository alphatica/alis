package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.optimizer.Optimizable;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;

public abstract class SignalGenerator implements Optimizable {

    public abstract Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    public abstract boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet);

    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {}

    public void paramsChanged() {}

}

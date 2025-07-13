package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.util.concurrent.ThreadLocalRandom;

public class RandomTradeSignal implements TradeSignal {
    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return ThreadLocalRandom.current().nextFloat() < 0.05 ? 1.0f : Float.NaN;
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return ThreadLocalRandom.current().nextFloat() < 0.05;
    }

    @Override
    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {

    }
}

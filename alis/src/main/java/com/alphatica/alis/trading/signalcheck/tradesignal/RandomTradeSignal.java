package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.util.concurrent.ThreadLocalRandom;

public class RandomTradeSignal extends TradeSignal {
    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (ThreadLocalRandom.current().nextFloat() < 0.05) {
            return 1.0f;
        } else {
            return Float.NaN;
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return ThreadLocalRandom.current().nextFloat() < 0.05;
    }

}

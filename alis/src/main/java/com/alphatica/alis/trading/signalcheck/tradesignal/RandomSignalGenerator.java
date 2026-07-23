package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSignalGenerator extends SignalGenerator {
    @Override
    public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (ThreadLocalRandom.current().nextFloat() < 0.05) {
            return Optional.of(new BuySignal(1.0, 1.0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return ThreadLocalRandom.current().nextFloat() < 0.05;
    }

}

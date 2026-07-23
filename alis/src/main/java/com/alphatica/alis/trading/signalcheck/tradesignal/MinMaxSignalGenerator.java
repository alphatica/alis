package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;

public class MinMaxSignalGenerator extends SignalGenerator {
    private final MinMax minMax = new MinMax(250);

    @Override
    public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (minMax.calculate(marketData) > 0) {
            return Optional.of(new BuySignal(1.0, 1.0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return minMax.calculate(marketData) < 0;
    }

}

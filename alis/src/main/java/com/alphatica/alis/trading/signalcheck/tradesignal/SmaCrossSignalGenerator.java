package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;

public class SmaCrossSignalGenerator extends SignalGenerator {
    private final Sma smaShort = new Sma(50);
    private final Sma smaLong = new Sma(200);
    
    @Override
    public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (smaShort.calculate(marketData) > smaLong.calculate(marketData)) {
            return Optional.of(new BuySignal(1.0, 1.0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return smaShort.calculate(marketData) < smaLong.calculate(marketData);
    }

}

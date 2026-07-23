package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.condition.HighestClose;
import com.alphatica.alis.condition.LowestClose;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;

public class DonchianChannelSignalGenerator extends SignalGenerator {

    @IntParam(start = 10, step = 1, end = 400)
    private int highLength = 200;

    @IntParam(start = 10, step = 1, end = 400)
    private int lowLength = 50;

    private HighestClose highestClose;
    private LowestClose lowestClose;

    public DonchianChannelSignalGenerator() {
        paramsChanged();
    }

    @Override
    public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (highestClose.matches(marketData, marketDataSet)) {
            return Optional.of(new BuySignal(1.0, 1.0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return lowestClose.matches(marketData, marketDataSet);
    }

    @Override
    public void paramsChanged() {
        highestClose = new HighestClose(highLength);
        lowestClose = new LowestClose(lowLength);
    }
}

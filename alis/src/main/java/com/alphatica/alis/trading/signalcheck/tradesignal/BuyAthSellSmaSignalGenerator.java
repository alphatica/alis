package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import com.alphatica.alis.trading.signalcheck.BuySignal;

import java.util.Optional;

public class BuyAthSellSmaSignalGenerator extends SignalGenerator {
    private final AllTimeHigh ath = new AllTimeHigh();

    @IntParam(start = 20, step = 1, end = 500)
    int smaLength = 200;

    private Sma sma = new Sma(smaLength);

    @Override
    public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (ath.matches(marketData, marketDataSet)) {
            return Optional.of(new BuySignal(1.0, 1.0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        var closeNow = marketData.getData(Layer.CLOSE, 0);
        return closeNow < sma.calculate(marketData);
    }

    @Override
    public void paramsChanged() {
        sma = new Sma(smaLength);
    }
}

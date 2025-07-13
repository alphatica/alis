package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.condition.HighestClose;
import com.alphatica.alis.condition.LowestClose;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public class DonchianChannelSignal implements TradeSignal {

    private final HighestClose highestClose = new HighestClose(200);
    private final LowestClose lowestClose = new LowestClose(50);

    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return highestClose.matches(marketData, marketDataSet) ? 1.0f : Float.NaN;
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return lowestClose.matches(marketData, marketDataSet);
    }

    @Override
    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {

    }
}

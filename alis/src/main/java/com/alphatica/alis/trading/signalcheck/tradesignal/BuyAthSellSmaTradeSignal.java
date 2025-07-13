package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;

public class BuyAthSellSmaTradeSignal implements TradeSignal {
    private final AllTimeHigh ath = new AllTimeHigh();
    private final Sma sma = new Sma(200);

    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        var oneYearLength = 250;
        var closes = marketData.getLayer(Layer.CLOSE);
        if (closes.size() <= oneYearLength) {
            return Float.NaN;
        }
        if (closes.get(0) < sma.calculate(marketData)) {
            return Float.NaN;
        }
        return ath.matches(marketData, marketDataSet) ? 1.0f : Float.NaN;
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        var closeNow = marketData.getData(Layer.CLOSE, 0);
        return closeNow < sma.calculate(marketData);
    }

    @Override
    public void afterClose(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {}

}

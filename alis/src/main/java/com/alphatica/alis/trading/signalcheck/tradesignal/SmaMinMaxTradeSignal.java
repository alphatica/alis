package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.indicators.trend.Sma;

public class SmaMinMaxTradeSignal extends TradeSignal {
    private final Sma sma = new Sma(200);
    private final MinMax minMax = new MinMax(250);

    @Override
    public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        if (marketData.getData(Layer.CLOSE, 0) > sma.calculate(marketData) && minMax.calculate(marketData) > 0) {
            return 1.0f;
        } else {
            return Float.NaN;
        }
    }

    @Override
    public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
        return marketData.getData(Layer.CLOSE, 0) < sma.calculate(marketData)
                || minMax.calculate(marketData) < 0;
    }

}

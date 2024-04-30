package com.alphatica.alis.indicators.candlesticks;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.HighestHigh;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.indicators.trend.Sma;


public class BearishEngulfment implements Condition {

    private static final int LENGTH = 10;
    private final Indicator sma = new Sma(LENGTH);
    private final Condition highestHigh = new HighestHigh(LENGTH);


    @Override
    public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
        if (market.getLayer(Layer.CLOSE).size() < LENGTH) {
            return false;
        }
        if (sma.calculate(market).orElse(0.0) > market.getData(Layer.CLOSE, 0)) {
            return false;
        }
        if (!highestHigh.matches(market, allMarkets)) {
            return false;
        }
        return market.getData(Layer.CLOSE, 0) < market.getData(Layer.LOW, 1);
    }
}

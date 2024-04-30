package com.alphatica.alis.indicators.trend;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.DoubleArrayRange;

import java.util.Optional;

public class Sma implements Indicator {
    private final int length;

    public Sma(int length) {
        this.length = length;
    }

    @Override
    public Optional<Double> calculate(TimeMarketData marketData) {
        DoubleArrayRange closes = marketData.getLayer(Layer.CLOSE);
        if (closes.size() < length) {
            return Optional.empty();
        } else {
            double sum = 0.0;
            for (int i = 0; i < length; i++) {
                sum += closes.get(i);
            }
            return Optional.of(sum / length);
        }
    }
}

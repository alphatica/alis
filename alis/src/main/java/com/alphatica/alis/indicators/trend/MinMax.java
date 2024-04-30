package com.alphatica.alis.indicators.trend;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.DoubleArrayRange;

import java.util.Optional;

public class MinMax implements Indicator {
    private final int length;

    public MinMax(int length) {
        this.length = length;
    }

    @Override
    public Optional<Double> calculate(TimeMarketData marketData) {
        DoubleArrayRange closes = marketData.getLayer(Layer.CLOSE);
        if (closes.size() < length) {
            return Optional.empty();
        } else {
            double max = closes.get(0);
            int indexMax = 0;
            double min = closes.get(0);
            int indexMin = 0;
            for (int i = 1; i < length; i++) {
                double now = closes.get(i);
                if (now < min) {
                    min = now;
                    indexMin = i;
                }
                if (now > max) {
                    max = now;
                    indexMax = i;
                }
            }
            return Optional.of(((double) indexMin - indexMax) / length);
        }
    }
}

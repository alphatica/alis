package com.alphatica.alis.indicators;

import com.alphatica.alis.data.time.TimeMarketData;

import java.util.Optional;

public interface Indicator {
    Optional<Double> calculate(TimeMarketData marketData);
}
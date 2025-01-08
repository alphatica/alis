package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.data.time.TimeMarketData;

public record MarketPriority(TimeMarketData data, double priority) {

}

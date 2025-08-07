package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

public record PositionReport(String sourceClass, Time time, MarketName market, float positionSize) {
}

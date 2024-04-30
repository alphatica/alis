package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;

import java.util.List;

public record SingleMatchResult(Time time, Market market, Time openTime, double openPrice, Time closeTime,
								double closePrice, double change, List<LevelReachedStat> higherThanLevelsReached) {
}

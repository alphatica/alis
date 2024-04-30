package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.util.List;

public record ChangeCheckTask(Time conditionTime, Time startTime, Time endTime, List<Market> markets,
							  TimeMarketDataSet marketDataSet, ChangeCheck changeCheck) {
}

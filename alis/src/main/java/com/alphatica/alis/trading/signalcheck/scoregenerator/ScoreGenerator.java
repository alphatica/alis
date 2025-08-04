package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.signalcheck.OpenTrade;

import java.util.List;
import java.util.Map;

public abstract class ScoreGenerator {

    public void onDone() {}

    public abstract void afterTrade(OpenTrade trade, float effectiveClosePrice);

    public void afterTime(TimeMarketDataSet marketDataSet, Map<MarketName, List<OpenTrade>> openTradeMap) {}

    public abstract double score();

}

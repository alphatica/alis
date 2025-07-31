package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.signalcheck.OpenTrade;

public abstract class ScoreGenerator {

    public void onDone() {}

    public abstract void afterTrade(OpenTrade trade, float effectiveClosePrice);

    public void afterTime(TimeMarketDataSet marketDataSet) {};

    public abstract double score();

}

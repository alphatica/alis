package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.OpenTrade;

public abstract class ScoreGenerator {
    public void onDone() {}

    public void afterTrade(OpenTrade trade, float effectiveClosePrice) {}
}

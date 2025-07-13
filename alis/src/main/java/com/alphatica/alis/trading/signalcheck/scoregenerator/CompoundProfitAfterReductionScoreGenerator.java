package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.OpenTrade;

import java.util.concurrent.atomic.AtomicInteger;

public class CompoundProfitAfterReductionScoreGenerator extends ScoreGenerator {
    private static final int REDUCTION_FACTOR = 20;
    private final AtomicInteger bars = new AtomicInteger(0);
    private final AtomicInteger trades = new AtomicInteger(0);

    private volatile double sumOfLogReturns = 0;

    @Override
    public void afterTrade(OpenTrade trade, float effectiveClosePrice) {
        var tradeReturn = (((double) effectiveClosePrice / trade.getOpenPrice() - 1) * trade.getPosition());
        var change = Math.log(1 + (tradeReturn / REDUCTION_FACTOR));
        synchronized (this) {
            sumOfLogReturns += change;
        }
        trades.incrementAndGet();
        bars.addAndGet(trade.getBars());
    }

    @Override
    public void onDone() {
        var geometricMeanTradeReturn = Math.exp(sumOfLogReturns / trades.get());
        System.out.printf("Geometric mean trade return at reduction factor %d: %.5f%n", REDUCTION_FACTOR, geometricMeanTradeReturn);
        var averageTradeDuration = (double)bars.get() / trades.get();
        var tradesInYear = 250 / averageTradeDuration;
        System.out.println("Trades in a year on average " + tradesInYear);
        var annual = (Math.pow(geometricMeanTradeReturn, tradesInYear) - 1) * REDUCTION_FACTOR * 100;
        System.out.printf("Annual growth (%%) after reduction factor %d: %.1f%n", REDUCTION_FACTOR, annual);
    }

}

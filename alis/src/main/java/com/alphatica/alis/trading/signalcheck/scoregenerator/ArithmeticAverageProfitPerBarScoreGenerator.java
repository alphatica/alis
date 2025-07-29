package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.OpenTrade;

import java.util.concurrent.atomic.AtomicInteger;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public class ArithmeticAverageProfitPerBarScoreGenerator extends ScoreGenerator {
    private final AtomicInteger bars = new AtomicInteger(0);
    private final AtomicInteger trades = new AtomicInteger(0);

    private volatile double profit;

    @Override
    public void afterTrade(OpenTrade trade, float effectiveClosePrice) {
        var tradeProfit = percentChange(trade.getOpenPrice(), effectiveClosePrice) * trade.getPositionSize();
        synchronized (this) {
            profit += tradeProfit;
        }
        trades.incrementAndGet();
        bars.addAndGet(trade.getBars());
    }

    @Override
    public double score() {
        return profit / bars.get();
    }

    public void show() {
        System.out.printf("Total bars: %d%n".formatted(bars.get()));
        System.out.printf("Total trades: %d%n".formatted(trades.get()));

        var averageTradeDuration = (double)bars.get() / trades.get();
        System.out.printf("Average trade duration %.2f%n".formatted(averageTradeDuration));

        var averageTradeProfit = profit / trades.get();
        System.out.printf("Average trade profit: %.5f%n".formatted(averageTradeProfit));

        var averageProfitPerBar = profit / bars.get();
        System.out.printf("Average profit per bar: %.3f%n".formatted(averageProfitPerBar));
    }
}

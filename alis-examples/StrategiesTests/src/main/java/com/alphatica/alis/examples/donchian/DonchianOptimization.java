package com.alphatica.alis.examples.donchian;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.GreenThreadExecutor;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DonchianOptimization {

    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        MarketData stooqData = StooqLoader.load(WORK_DIR);
        boolean bestGrowthSort = true;
        int bestBuyPeriod = 0;
        int bestSellPeriod = 0;
        double bestAccountNav = 0;
        GreenThreadExecutor<DonchianResult, DonchianResult> parallelExecutor = getDonchianParallelExecutor(stooqData);
        System.out.println("Waiting for results...");
        List<DonchianResult> results = parallelExecutor.results();
        for (DonchianResult result : results) {
            if (result.finalNav() > bestAccountNav) {
                bestAccountNav = result.finalNav();
                bestSellPeriod = result.sellPeriod();
                bestBuyPeriod = result.buyPeriod();
                bestGrowthSort = result.sortByHighestGrowth();
            }
        }
        System.out.printf("Best: %.0f %d %d %s", bestAccountNav, bestSellPeriod, bestBuyPeriod, bestGrowthSort);
    }

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    private static GreenThreadExecutor<DonchianResult, DonchianResult> getDonchianParallelExecutor(MarketData stooqData) {
        double initialCash = 100_000;
        GreenThreadExecutor<DonchianResult, DonchianResult> donchianParallelExecutor = new GreenThreadExecutor<>(p -> {
            StrategyExecutor executor = new StrategyExecutor().withInitialCash(initialCash).withTimeRange(new Time(2007_07_09), new Time(2018_02_01));
            Account account = executor.execute(stooqData, new DonchianStrategy(p.buyPeriod(), p.sellPeriod(), p.sortByHighestGrowth(), false));
            System.out.printf("%d %d %s %.0f%n", p.buyPeriod(), p.sellPeriod(), p.sortByHighestGrowth(), account.getNAV());
            return new DonchianResult(p.buyPeriod(), p.sellPeriod(), p.sortByHighestGrowth(), account.getNAV());
        });
        int sellPeriod = 5;
        while (sellPeriod <= 375) {
            int buyPeriod = 5;
            while (buyPeriod < 375) {
                donchianParallelExecutor.submit(new DonchianResult(buyPeriod, sellPeriod, true, 0.0));
                donchianParallelExecutor.submit(new DonchianResult(buyPeriod, sellPeriod, false, 0.0));
                buyPeriod = next(buyPeriod);
            }
            sellPeriod = next(sellPeriod);
        }
        return donchianParallelExecutor;
    }

    private static int next(int period) {
        int next = (int) Math.round(period * 1.02);
        if (next == period) {
            return period + 1;
        } else {
            return next;
        }
    }
}

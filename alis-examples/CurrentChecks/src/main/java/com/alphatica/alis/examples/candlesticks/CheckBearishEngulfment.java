package com.alphatica.alis.examples.candlesticks;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.candlesticks.BearishEngulfment;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilter.STOCKS;

public class CheckBearishEngulfment {
    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        MarketData stooqData = StooqLoader.load(WORK_DIR);
        Condition bearishEngulfment = new BearishEngulfment();
        Time last = stooqData.getTimes().getLast();
        TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(last, stooqData);
        for (TimeMarketData marketData : timeMarketDataSet.listMarkets()) {
            if (bearishEngulfment.matches(marketData, timeMarketDataSet)) {
                System.out.println(marketData.getMarketName());
            }
        }
        ChangeCheck changeCheck = ChangeCheck.condition(bearishEngulfment).marketFilter(STOCKS).windowLength(10).from(new Time(2000_01_01));
        ChangeCheckResult results = ChangeCheckExecutor.execute(changeCheck, stooqData);
        System.out.println("Average: " + results.average());
        System.out.println("Median: " + results.median());
        System.out.println("Time average: " + results.timeAverage());
        System.out.println("Count: " + results.count());

    }
}

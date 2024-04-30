package com.alphatica.alis.examples.changecheck;


import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.HorizontalLine;
import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.LiquidityMoreThan;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilter.STOCKS;

public class CheckChangeAfterAllTimeHigh {
    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        MarketData stooqData = StooqLoader.load(WORK_DIR);
        Condition condition = Condition.all(new AllTimeHigh(), new LiquidityMoreThan(50_000.0, 5));
        ChangeCheck changeCheck = ChangeCheck.condition(condition).marketFilter(STOCKS).windowLength(250).from(new Time(2000_01_01));
        ChangeCheckResult results = ChangeCheckExecutor.execute(changeCheck, stooqData);
        System.out.println("Average: " + results.average());
        System.out.println("Median: " + results.median());
        System.out.println("Time average: " + results.timeAverage());
        System.out.println("Count: " + results.count());

        results.removeOverlapping();
        System.out.println("After removing overlapping:");
        System.out.println("Average: " + results.average());
        System.out.println("Median: " + results.median());
        System.out.println("Time average: " + results.timeAverage());
        System.out.println("Count: " + results.count());

        Chart<Time> chart = new Chart<>();
        chart.addDataLines(results.getChartLines());
        results.average().ifPresent(average -> chart.addHorizontalLine(new HorizontalLine("Average", average)));
        chart.setCopyright("Alphatica.com");
        chart.setTitle("1-Year change after ATH");
        chart.setXName("Date");
        chart.setYName("% Change");
        chart.setLogarithmic(true);
        chart.createImage(new File("1yearAth.png"));
    }
}

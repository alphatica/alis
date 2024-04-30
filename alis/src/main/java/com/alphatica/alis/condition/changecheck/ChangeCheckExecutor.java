package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.time.TimeRangeMarketData;
import com.alphatica.alis.tools.java.GreenThreadExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.layer.Layer.OPEN;
import static java.util.Optional.empty;

public class ChangeCheckExecutor {
    private ChangeCheckExecutor() {
    }

    public static ChangeCheckResult execute(ChangeCheck changeCheck, MarketData marketData) throws ExecutionException, InterruptedException {
        List<Time> times = marketData.getTimes();
        List<Market> markets = marketData.listMarkets(changeCheck.getMarketFilter()).stream().toList();
        int benchmarkStartTimeIndex = getBenchmarkStartTimeIndex(changeCheck, times);
        GreenThreadExecutor<ChangeCheckTask, List<SingleMatchResult>> executor = new GreenThreadExecutor<>(ChangeCheckExecutor::checkBenchmark);
        submitChecks(changeCheck, marketData, benchmarkStartTimeIndex, times, executor, markets);
        TreeMap<Time, List<SingleMatchResult>> results = collectResults(executor);
        return new ChangeCheckResult(results);
    }

    private static TreeMap<Time, List<SingleMatchResult>> collectResults(GreenThreadExecutor<ChangeCheckTask, List<SingleMatchResult>> executor) throws ExecutionException, InterruptedException {
        TreeMap<Time, List<SingleMatchResult>> results = new TreeMap<>();
        for (List<SingleMatchResult> timeResults : executor.results()) {
            if (!timeResults.isEmpty()) {
                results.put(timeResults.getFirst().time(), timeResults);
            }
        }
        return results;
    }

    @SuppressWarnings("java:S135") // Allow multiple breaks
    private static void submitChecks(ChangeCheck changeCheck, MarketData marketData, int benchmarkStartTimeIndex, List<Time> times, GreenThreadExecutor<ChangeCheckTask, List<SingleMatchResult>> executor, List<Market> markets) {
        for (int i = benchmarkStartTimeIndex; i < times.size(); i++) {
            Time conditionTime = times.get(i);
            if (conditionTime.isAfter(changeCheck.getTo())) {
                break;
            }
            int startTimeIndex = i + 1;
            int endTimeIndex = startTimeIndex + changeCheck.getWindowLength();
            if (endTimeIndex >= times.size()) {
                break;
            }
            Time startTime = times.get(startTimeIndex);
            Time endTime = times.get(endTimeIndex);
            TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(conditionTime, marketData);
            executor.submit(new ChangeCheckTask(conditionTime, startTime, endTime, markets, timeMarketDataSet, changeCheck));
        }
    }

    private static int getBenchmarkStartTimeIndex(ChangeCheck changeCheck, List<Time> times) {
        int benchmarkStartTimeIndex = Collections.binarySearch(times, changeCheck.getFrom());
        if (benchmarkStartTimeIndex < 0) {
            benchmarkStartTimeIndex = -benchmarkStartTimeIndex - 1;
        }
        return benchmarkStartTimeIndex;
    }

    @SuppressWarnings("java:S135") // Allow multiple continue
    private static List<SingleMatchResult> checkBenchmark(ChangeCheckTask task) {
        List<SingleMatchResult> results = new ArrayList<>(1024);
        for (Market market : task.markets()) {
            TimeMarketData marketData = task.marketDataSet().get(market.getName());
            if (marketData == null) {
                continue;
            }
            if (!task.changeCheck().getCondition().matches(marketData, task.marketDataSet())) {
                continue;
            }
            Optional<TimeRangeMarketData> maybeTimeRangeMarketData = getTimeRangeMarketData(market, task.startTime(), task.endTime());
            if (maybeTimeRangeMarketData.isEmpty()) {
                continue;
            }
            TimeRangeMarketData timeRangeMarketData = maybeTimeRangeMarketData.get();
            double startPrice = timeRangeMarketData.start().getData(OPEN, 0);
            double endPrice = timeRangeMarketData.end().getData(OPEN, 0);
            double change = ((endPrice / startPrice) - 1.0) * 100;
            results.add(new SingleMatchResult(task.conditionTime(), market, timeRangeMarketData.start().getTime(), startPrice, timeRangeMarketData.end().getTime(), endPrice, change));
        }
        return results;
    }

    private static Optional<TimeRangeMarketData> getTimeRangeMarketData(Market market, Time startTime, Time endTime) {
        Optional<TimeMarketData> maybeStartData = market.getAt(startTime);
        if (maybeStartData.isEmpty()) {
            return empty();
        }
        TimeMarketData startData = maybeStartData.get();

        Optional<TimeMarketData> maybeEndData = market.getAt(endTime);
        if (maybeEndData.isEmpty()) {
            return empty();
        }
        TimeMarketData endData = maybeEndData.get();
        return Optional.of(new TimeRangeMarketData(startData, endData));

    }
}

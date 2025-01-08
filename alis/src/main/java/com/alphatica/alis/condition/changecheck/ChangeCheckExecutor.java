package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.time.TimeRangeMarketData;
import com.alphatica.alis.tools.java.TaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class ChangeCheckExecutor {
	private ChangeCheckExecutor() {
	}

	public static ChangeCheckResult execute(ChangeCheck changeCheck, MarketData marketData) throws ExecutionException, InterruptedException {
		List<Time> times = marketData.getTimes();
		List<Market> markets = marketData.listMarkets(changeCheck.getMarketFilter()).stream().toList();
		int benchmarkStartTimeIndex = getBenchmarkStartTimeIndex(changeCheck, times);
		TaskExecutor<List<SingleMatchResult>> executor = new TaskExecutor<>();
		submitChecks(changeCheck, marketData, benchmarkStartTimeIndex, times, markets, executor);
		TreeMap<Time, List<SingleMatchResult>> results = collectResults(executor);
		return new ChangeCheckResult(results, changeCheck.getMovesLevels());

	}

	private static TreeMap<Time, List<SingleMatchResult>> collectResults(TaskExecutor<List<SingleMatchResult>> executor) throws ExecutionException,
			InterruptedException {
		TreeMap<Time, List<SingleMatchResult>> results = new TreeMap<>();
		for (List<SingleMatchResult> timeResults : executor.getResults()) {
			if (!timeResults.isEmpty()) {
				results.put(timeResults.getFirst().time(), timeResults);
			}
		}
		return results;
	}

	@SuppressWarnings("java:S135") // Allow multiple breaks
	private static void submitChecks(ChangeCheck changeCheck, MarketData marketData, int benchmarkStartTimeIndex, List<Time> times,
									 List<Market> markets, TaskExecutor<List<SingleMatchResult>> executor) {
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
			executor.submit(() -> checkCondition(new ChangeCheckTask(conditionTime, startTime, endTime, markets, timeMarketDataSet, changeCheck)));
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
	private static List<SingleMatchResult> checkCondition(ChangeCheckTask task) {
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
			if (Double.isFinite(change)) {
				List<LevelReachedStat> higherThanLevelsReached = getLevelsReached(task.changeCheck()
																					  .getMovesLevels(), startPrice, market, task.startTime(),
						task.endTime());
				results.add(new SingleMatchResult(task.conditionTime(), market, timeRangeMarketData.start()
																								   .getTime(), startPrice, timeRangeMarketData.end()
																																			  .getTime(), endPrice, change, higherThanLevelsReached));
			}
		}
		return results;
	}

	private static List<LevelReachedStat> getLevelsReached(List<Double> levels, double startPrice, Market market, Time startTime, Time endTime) {
		List<LevelReachedStat> higherThanLevelsReached = buildLevelsReachedList(levels);
		List<Double> higherThanLevels = buildLevelsValues(levels, startPrice);
		Time checkTime = startTime;
		int nextLevelToFind = 0;
		int sessions = 0;
		while (!checkTime.isAfter(endTime) && nextLevelToFind < higherThanLevels.size()) {
			Optional<TimeMarketData> maybeData = getDataForTime(market, checkTime, endTime);
			if (maybeData.isPresent()) {
				TimeMarketData data = maybeData.get();
				sessions++;
				for (int level = nextLevelToFind; level < higherThanLevels.size(); level++) {
					if (data.getData(HIGH, 0) > higherThanLevels.get(level)) {
						higherThanLevelsReached.set(level, new LevelReachedStat(true, sessions));
						nextLevelToFind++;
					}
				}
				checkTime = data.getTime().next();
			}
		}
		return higherThanLevelsReached;
	}

	private static Optional<TimeMarketData> getDataForTime(Market market, Time checkTime, Time endTime) {
		TimeMarketData data = market.getAtOrNext(checkTime);
		if (data != null && !data.getTime().isAfter(endTime)) {
			return of(data);
		}

		return Optional.empty();
	}

	private static List<Double> buildLevelsValues(List<Double> levels, double startPrice) {
		List<Double> higherThanLevels = new ArrayList<>(levels.size());
		for (Double level : levels) {
			higherThanLevels.add(startPrice * (1 + (level / 100)));
		}
		return higherThanLevels;
	}

	private static List<LevelReachedStat> buildLevelsReachedList(List<Double> levels) {
		List<LevelReachedStat> higherThanLevelsReached = new ArrayList<>(levels.size());
		for (int i = 0; i < levels.size(); i++) {
			higherThanLevelsReached.add(new LevelReachedStat(false, 0));
		}
		return higherThanLevelsReached;
	}

	private static Optional<TimeRangeMarketData> getTimeRangeMarketData(Market market, Time startTime, Time endTime) {
		TimeMarketData startData = market.getAt(startTime);
		if (startData == null) {
			return empty();
		}

		TimeMarketData endData = market.getAt(endTime);
		if (endData == null) {
			return empty();
		}
		return of(new TimeRangeMarketData(startData, endData));
	}
}

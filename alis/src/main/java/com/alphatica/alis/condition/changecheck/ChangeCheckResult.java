package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.math.Statistics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record ChangeCheckResult(TreeMap<Time, List<SingleMatchResult>> results, List<Double> levels) {

	private static List<LevelReachedProbability> getLevelReachedProbabilities(List<Double> levels, List<Integer> reachedCounters, List<Integer> sessionCounters, int count) {
		List<LevelReachedProbability> ratios = new ArrayList<>();
		if (reachedCounters != null) {
			for (int i = 0; i < reachedCounters.size(); i++) {
				int levelReachedCount = reachedCounters.get(i);
				double averageSessions = (double) sessionCounters.get(i) / levelReachedCount;
				double ratio = (double) levelReachedCount / count;
				LevelReachedProbability probability = new LevelReachedProbability(levels.get(i), ratio, averageSessions);
				ratios.add(probability);
			}
		}
		return ratios;
	}

	public void showStats(PrintStream stream) {
		stream.println("Average: " + average());
		stream.println("Median: " + median());
		stream.println("Time average: " + timeAverage());
		stream.println("Count: " + count());
		List<LevelReachedProbability> levelReachedProbabilities = movesLevelsProbabilities();
		for (int i = 0; i < levelReachedProbabilities.size(); i++) {
			stream.printf("%.0f%%: %.2f %.0f%n", levels.get(i), levelReachedProbabilities.get(i)
																						 .ratio(), levelReachedProbabilities.get(i)
																															.averageSessions());
		}
	}

	public Optional<Double> average() {
		double sum = 0.0;
		int count = 0;
		for (List<SingleMatchResult> singleMatchResults : results.values()) {
			count += singleMatchResults.size();
			for (SingleMatchResult singleMatchResult : singleMatchResults) {
				sum += singleMatchResult.change();
			}
		}
		if (count == 0) {
			return Optional.empty();
		} else {
			return Optional.of(sum / count);
		}

	}

	public Optional<Double> median() {
		List<Double> values = results.values()
									 .stream()
									 .flatMap(List::stream)
									 .mapToDouble(SingleMatchResult::change)
									 .boxed()
									 .sorted()
									 .toList();
		if (values.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(Statistics.median(values));
		}
	}

	public List<LevelReachedProbability> movesLevelsProbabilities() {
		List<List<LevelReachedStat>> reachedLists = results.values()
														   .stream()
														   .flatMap(List::stream)
														   .map(SingleMatchResult::higherThanLevelsReached)
														   .toList();
		int count = 0;
		List<Integer> reachedCounters = null;
		List<Integer> sessionCounters = null;
		for (List<LevelReachedStat> singleReachedStat : reachedLists) {
			count++;
			if (reachedCounters == null) {
				reachedCounters = new ArrayList<>();
				sessionCounters = new ArrayList<>();
				for (int i = 0; i < singleReachedStat.size(); i++) {
					reachedCounters.add(0);
					sessionCounters.add(0);
				}
			}
			for (int i = 0; i < singleReachedStat.size(); i++) {
				if (singleReachedStat.get(i).reached()) {
					reachedCounters.set(i, reachedCounters.get(i) + 1);
					sessionCounters.set(i, sessionCounters.get(i) + singleReachedStat.get(i).sessions());
				}
			}
		}
		return getLevelReachedProbabilities(levels, reachedCounters, sessionCounters, count);
	}

	public void removeOverlapping() {
		HashMap<MarketName, Time> current = new HashMap<>();
		for (Map.Entry<Time, List<SingleMatchResult>> timeResults : results.entrySet()) {
			current.entrySet().removeIf(entry -> entry.getValue().isBefore(timeResults.getKey()));
			timeResults.getValue().removeIf(result -> current.containsKey(result.market().getName()));
			timeResults.getValue().forEach(result -> current.put(result.market().getName(), result.closeTime()));
		}
		results.entrySet().removeIf(timeResults -> timeResults.getValue().isEmpty());
	}

	public Optional<Double> timeAverage() {
		double timeSum = 0.0;
		int count = 0;
		for (List<SingleMatchResult> singleMatchResults : results.values()) {
			count++;
			double sum = singleMatchResults.stream().mapToDouble(SingleMatchResult::change).sum();
			timeSum += sum / singleMatchResults.size();
		}
		if (count == 0) {
			return Optional.empty();
		} else {
			return Optional.of(timeSum / count);
		}
	}

	public int count() {
		return results.values().stream().mapToInt(List::size).sum();
	}

	public List<LineChartData<Time>> getChartLines() {
		LineChartData<Time> timeChartLine = new LineChartData<>();
		timeChartLine.setName("Changes");
		results.forEach((key, value) -> value.forEach(result -> timeChartLine.addPoint(key, result.change())));
		return List.of(timeChartLine);
	}
}

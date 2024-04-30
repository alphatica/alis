package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.math.Statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public record ChangeCheckResult(TreeMap<Time, List<SingleMatchResult>> results) {

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
        List<Double> values = results.values().stream().flatMap(List::stream).mapToDouble(SingleMatchResult::change).boxed().toList();
        if (values.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(Statistics.median(values));
        }
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

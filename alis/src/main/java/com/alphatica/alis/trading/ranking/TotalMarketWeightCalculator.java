package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.MarketScore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;

import static java.util.Collections.emptyList;

public class TotalMarketWeightCalculator {
	private final SourceWeightMap sourceWeightMap;
	private final PositionReporter positionReporter;

	public TotalMarketWeightCalculator(SourceWeightMap sourceWeightMap, PositionReporter positionReporter) {
		this.sourceWeightMap = sourceWeightMap;
		this.positionReporter = positionReporter;
	}

	public double totalMarketWeight(Time time, MarketName marketName) {
		double weight = 0;
		for(String source: sourceWeightMap.sources()) {
			var position = positionReporter.getPosition(time, source, marketName);
			weight += position * sourceWeightMap.getWeight(source);
		}
		return weight;
	}

	public List<MarketScore> getRanking(Time time) {
		Map<String, Map<MarketName, DoubleAdder>> sourcesPositions = positionReporter.getContent().get(time);
		if (sourcesPositions == null) {
			return emptyList();
		}

		Map<MarketName, Double> scores = new HashMap<>();
		for (var weightEntry : sourceWeightMap.getContent().entrySet()) {
			String source = weightEntry.getKey();
			double weight = weightEntry.getValue();
			Map<MarketName, DoubleAdder> positions = sourcesPositions.get(source);
			if (positions != null) {
				for (var positionEntry : positions.entrySet()) {
					MarketName market = positionEntry.getKey();
					double positionValue = positionEntry.getValue().doubleValue();
					scores.compute(market, (k, v) -> (v == null ? 0 : v) + weight * positionValue);
				}
			}
		}

		List<MarketScore> result = new ArrayList<>();
		for (var entry : scores.entrySet()) {
			result.add(new MarketScore(entry.getKey(), entry.getValue()));
		}
		result.sort(Comparator.reverseOrder());
		return result;
	}

}

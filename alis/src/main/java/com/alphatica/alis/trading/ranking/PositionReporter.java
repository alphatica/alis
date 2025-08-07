package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

import static java.util.Optional.ofNullable;

public class PositionReporter {
	// Every time has map of every source name with every market name with list of position sizes
	private final Map<Time, Map<String, Map<MarketName, DoubleAdder>>> content = new ConcurrentHashMap<>();

	public void report(PositionReport report) {
		content
				.computeIfAbsent(report.time(), t -> new ConcurrentHashMap<>())
				.computeIfAbsent(report.sourceClass(), s -> new ConcurrentHashMap<>())
				.computeIfAbsent(report.market(), m -> new DoubleAdder())
				.add(report.positionSize());
	}

	public Map<Time, Map<String, Map<MarketName, DoubleAdder>>> getContent() {
		return content;
	}

	public double getPosition(Time time, String source, MarketName marketName) {
		return ofNullable(content.get(time))
				.flatMap(m -> ofNullable(m.get(source)))
				.flatMap(m -> ofNullable(m.get(marketName)))
				.map(DoubleAdder::doubleValue).orElse(0.0);
	}
}

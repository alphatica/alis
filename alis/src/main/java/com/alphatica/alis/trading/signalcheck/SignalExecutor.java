package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.ranking.PositionReporter;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class SignalExecutor {
	private Time timeFrom = new Time(0);
	private Time timeTo = new Time(Integer.MAX_VALUE);
	private Predicate<TimeMarketData> marketFilter = ignored -> true;
	private double commissionRate = 0.01;
	private boolean tradeSecondarySignals;
	private boolean verbose;
	private PositionReporter positionReporter;
	private String sourceId;
	private boolean useCachedMarketData;

	public SignalExecutor withTimeRange(Time timeFrom, Time timeTo) {
		this.timeFrom = requireNonNull(timeFrom, "timeFrom");
		this.timeTo = requireNonNull(timeTo, "timeTo");
		if (timeTo.isBefore(timeFrom)) {
			throw new IllegalArgumentException("timeTo must not be before timeFrom");
		}
		return this;
	}

	public SignalExecutor withMarketFilter(Predicate<TimeMarketData> marketFilter) {
		this.marketFilter = requireNonNull(marketFilter, "marketFilter");
		return this;
	}

	public SignalExecutor withCommissionRate(double commissionRate) {
		if (!Double.isFinite(commissionRate) || commissionRate < 0.0 || commissionRate >= 1.0) {
			throw new IllegalArgumentException("commissionRate must be finite and in [0, 1)");
		}
		this.commissionRate = commissionRate;
		return this;
	}

	public SignalExecutor withSecondarySignals(boolean enabled) {
		tradeSecondarySignals = enabled;
		return this;
	}

	public SignalExecutor withPositionReporter(PositionReporter positionReporter, String sourceId) {
		this.positionReporter = requireNonNull(positionReporter, "positionReporter");
		this.sourceId = requireNonNull(sourceId, "sourceId");
		return this;
	}

	public SignalExecutor withVerbose(boolean enabled) {
		verbose = enabled;
		return this;
	}

	public SignalExecutor useCachedMarketData() {
		useCachedMarketData = true;
		return this;
	}

	public SignalExecutionResult execute(MarketData marketData, Supplier<SignalGenerator> signalGeneratorFactory) {
		requireNonNull(marketData, "marketData");
		requireNonNull(signalGeneratorFactory, "signalGeneratorFactory");
		List<Time> executionTimes = marketData.getTimes().stream()
				.filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo))
				.sorted()
				.toList();
		SignalTradeDiscovery discovery = new SignalTradeDiscovery(marketData, commissionRate,
				tradeSecondarySignals, positionReporter, sourceId, verbose);
		for (int eventIndex = 0; eventIndex < executionTimes.size(); eventIndex++) {
			processTime(marketData, signalGeneratorFactory, discovery, executionTimes.get(eventIndex), eventIndex);
		}
		discovery.closeLastTrades(executionTimes.size());
		return new SignalExecutionResult(timeFrom, timeTo, executionTimes, discovery.opportunities());
	}

	private void processTime(MarketData marketData, Supplier<SignalGenerator> signalGeneratorFactory,
			SignalTradeDiscovery discovery, Time time, int eventIndex) {
		TimeMarketDataSet marketDataSet = getTimeMarketDataSet(marketData, time);
		List<TimeMarketData> currentMarkets = marketDataSet.listUpToDateMarkets(marketFilter).stream()
				.sorted(Comparator.comparing(TimeMarketData::getMarketName))
				.toList();
		discovery.process(time, marketDataSet, currentMarkets, signalGeneratorFactory, eventIndex);
	}

	private TimeMarketDataSet getTimeMarketDataSet(MarketData marketData, Time time) {
		return useCachedMarketData ? marketData.cachedSnapshotAt(time) : marketData.snapshotAt(time);
	}

}

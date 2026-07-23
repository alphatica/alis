package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.ranking.PositionReport;
import com.alphatica.alis.trading.ranking.PositionReporter;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_CLOSE;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_OPEN;
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

	public SignalExecutionResult execute(MarketData marketData, Supplier<SignalGenerator> signalGeneratorSupplier) {
		requireNonNull(marketData, "marketData");
		requireNonNull(signalGeneratorSupplier, "signalGeneratorSupplier");
		List<Time> executionTimes = marketData.getTimes().stream()
				.filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo))
				.sorted()
				.toList();
		DiscoveryState state = new DiscoveryState(createTradeMap(marketData));
		for (int eventIndex = 0; eventIndex < executionTimes.size(); eventIndex++) {
			checkTime(marketData, signalGeneratorSupplier, state, executionTimes.get(eventIndex), eventIndex);
		}
		closeLastTrades(state, executionTimes.size());
		return new SignalExecutionResult(timeFrom, timeTo, executionTimes, state.opportunities);
	}

	private Map<MarketName, List<OpenTrade>> createTradeMap(MarketData marketData) {
		List<Market> markets = marketData.listMarkets(ALL).stream()
				.sorted(Comparator.comparing(Market::getName))
				.toList();
		Map<MarketName, List<OpenTrade>> result = new LinkedHashMap<>();
		for (Market market : markets) {
			result.put(market.getName(), new ArrayList<>());
		}
		return result;
	}

	private void checkTime(MarketData marketData, Supplier<SignalGenerator> signalGeneratorSupplier,
			DiscoveryState state, Time time, int eventIndex) {
		TimeMarketDataSet marketDataSet = getTimeMarketDataSet(marketData, time);
		List<TimeMarketData> currentMarkets = marketDataSet.listUpToDateMarkets(marketFilter).stream()
				.sorted(Comparator.comparing(TimeMarketData::getMarketName))
				.toList();
		log("%s =================================================", time);
		reportPositions(state.openTrades, time);

		for (TimeMarketData market : currentMarkets) {
			closePending(state, market, eventIndex);
		}
		for (TimeMarketData market : currentMarkets) {
			openPending(state.openTrades.get(market.getMarketName()), market, eventIndex);
		}
		for (TimeMarketData market : currentMarkets) {
			processOpenTrades(state.openTrades.get(market.getMarketName()), market, marketDataSet);
		}
		for (TimeMarketData market : currentMarkets) {
			checkNewSignals(signalGeneratorSupplier, state, market, marketDataSet);
		}
	}

	private TimeMarketDataSet getTimeMarketDataSet(MarketData marketData, Time time) {
		return useCachedMarketData ? marketData.cachedSnapshotAt(time) : marketData.snapshotAt(time);
	}

	private void reportPositions(Map<MarketName, List<OpenTrade>> openTrades, Time time) {
		if (positionReporter == null) {
			return;
		}
		for (Map.Entry<MarketName, List<OpenTrade>> entry : openTrades.entrySet()) {
			for (OpenTrade trade : entry.getValue()) {
				if (trade.getTradeStatus().countProfit()) {
					positionReporter.report(new PositionReport(sourceId, time, entry.getKey(),
							trade.getRequestedAllocation()));
				}
			}
		}
	}

	private void closePending(DiscoveryState state, TimeMarketData market, int eventIndex) {
		List<OpenTrade> trades = state.openTrades.get(market.getMarketName());
		Iterator<OpenTrade> iterator = trades.iterator();
		while (iterator.hasNext()) {
			OpenTrade trade = iterator.next();
			if (trade.getTradeStatus() == PENDING_CLOSE) {
				iterator.remove();
				float closePrice = (float) (market.getData(OPEN, 0) * (1.0 - commissionRate));
				recordClose(state, market.getMarketName(), trade, market.getTime(), eventIndex, closePrice);
			}
		}
	}

	private void openPending(List<OpenTrade> trades, TimeMarketData market, int eventIndex) {
		float openPrice = (float) (market.getData(OPEN, 0) * (1.0 + commissionRate));
		for (OpenTrade trade : trades) {
			if (trade.getTradeStatus() == PENDING_OPEN) {
				trade.open(openPrice, market.getTime(), eventIndex);
				log("Opening %s at %.2f allocation %.3f", market.getMarketName(), openPrice,
						trade.getRequestedAllocation());
			}
		}
	}

	private void processOpenTrades(List<OpenTrade> trades, TimeMarketData market,
			TimeMarketDataSet marketDataSet) {
		for (OpenTrade trade : trades) {
			if (trade.getTradeStatus() != TradeStatus.OPEN) {
				continue;
			}
			trade.updateLastKnownPrice(market.getData(CLOSE, 0), market.getTime());
			trade.incrementBars();
			SignalGenerator signalGenerator = trade.getSignalGenerator();
			signalGenerator.afterClose(market, marketDataSet);
			if (signalGenerator.shouldSell(market, marketDataSet)) {
				trade.setStatus(PENDING_CLOSE);
			}
		}
	}

	private void checkNewSignals(Supplier<SignalGenerator> signalGeneratorSupplier, DiscoveryState state,
			TimeMarketData market, TimeMarketDataSet marketDataSet) {
		List<OpenTrade> trades = state.openTrades.get(market.getMarketName());
		if (!trades.isEmpty() && !tradeSecondarySignals) {
			return;
		}
		SignalGenerator signalGenerator = requireNonNull(signalGeneratorSupplier.get(),
				"signalGeneratorSupplier result");
		signalGenerator.shouldBuy(market, marketDataSet).ifPresent(buySignal -> trades.add(
				new OpenTrade(signalGenerator, buySignal, market.getTime())));
	}

	private void closeLastTrades(DiscoveryState state, int finalEventIndex) {
		for (Map.Entry<MarketName, List<OpenTrade>> entry : state.openTrades.entrySet()) {
			for (OpenTrade trade : entry.getValue()) {
				if (trade.getTradeStatus() == TradeStatus.OPEN || trade.getTradeStatus() == PENDING_CLOSE) {
					float closePrice = (float) (trade.getLastKnownPrice() * (1.0 - commissionRate));
					recordClose(state, entry.getKey(), trade, trade.getLastKnownTime(), finalEventIndex, closePrice);
				}
			}
		}
	}

	private void recordClose(DiscoveryState state, MarketName market, OpenTrade trade, Time closeTime,
			int closeEventIndex, float closePrice) {
		state.opportunities.add(new TradeOpportunity(
				market, trade.getSignalTime(), trade.getOpenTime(), closeTime,
				trade.getOpenEventIndex(), closeEventIndex, trade.getEffectiveOpenPrice(), closePrice,
				trade.getBars(), trade.getRequestedAllocation(), trade.getPriority()));
		log("Closing %s at %.2f bought at %.2f profit %.2f", market, closePrice,
				trade.getEffectiveOpenPrice(), percentChange(trade.getEffectiveOpenPrice(), closePrice));
	}

	private void log(String format, Object... args) {
		if (verbose) {
			System.out.printf(format + "%n", args);
		}
	}

	private static final class DiscoveryState {
		private final Map<MarketName, List<OpenTrade>> openTrades;
		private final List<TradeOpportunity> opportunities = new ArrayList<>();

		private DiscoveryState(Map<MarketName, List<OpenTrade>> openTrades) {
			this.openTrades = openTrades;
		}
	}
}

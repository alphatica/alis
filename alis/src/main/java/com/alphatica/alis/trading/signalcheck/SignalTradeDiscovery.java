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
import java.util.function.Supplier;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_CLOSE;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_OPEN;
import static java.util.Objects.requireNonNull;

final class SignalTradeDiscovery {

	private final Map<MarketName, List<OpenTrade>> openTrades;
	private final List<TradeOpportunity> opportunities = new ArrayList<>();
	private final double commissionRate;
	private final boolean tradeSecondarySignals;
	private final PositionReporter positionReporter;
	private final String sourceId;
	private final boolean verbose;

	SignalTradeDiscovery(MarketData marketData, double commissionRate, boolean tradeSecondarySignals,
						 PositionReporter positionReporter, String sourceId, boolean verbose) {
		this.openTrades = createTradeMap(marketData);
		this.commissionRate = commissionRate;
		this.tradeSecondarySignals = tradeSecondarySignals;
		this.positionReporter = positionReporter;
		this.sourceId = sourceId;
		this.verbose = verbose;
	}

	void process(Time time, TimeMarketDataSet marketDataSet, List<TimeMarketData> currentMarkets,
				 Supplier<SignalGenerator> signalGeneratorFactory, int eventIndex) {
		log("%s =================================================", time);
		reportPositions(time);
		for (TimeMarketData market : currentMarkets) {
			closePending(market, eventIndex);
		}
		for (TimeMarketData market : currentMarkets) {
			openPending(openTrades.get(market.getMarketName()), market, eventIndex);
		}
		for (TimeMarketData market : currentMarkets) {
			processOpenTrades(openTrades.get(market.getMarketName()), market, marketDataSet);
		}
		for (TimeMarketData market : currentMarkets) {
			checkNewSignals(signalGeneratorFactory, market, marketDataSet);
		}
	}

	void closeLastTrades(int finalEventIndex) {
		for (Map.Entry<MarketName, List<OpenTrade>> entry : openTrades.entrySet()) {
			for (OpenTrade trade : entry.getValue()) {
				if (trade.getTradeStatus() == TradeStatus.OPEN || trade.getTradeStatus() == PENDING_CLOSE) {
					float closePrice = (float) (trade.getLastKnownPrice() * (1.0 - commissionRate));
					recordClose(entry.getKey(), trade, trade.getLastKnownTime(), finalEventIndex, closePrice);
				}
			}
		}
	}

	List<TradeOpportunity> opportunities() {
		return opportunities;
	}

	private static Map<MarketName, List<OpenTrade>> createTradeMap(MarketData marketData) {
		List<Market> markets = marketData.listMarkets(ALL).stream()
				.sorted(Comparator.comparing(Market::getName))
				.toList();
		Map<MarketName, List<OpenTrade>> result = new LinkedHashMap<>();
		for (Market market : markets) {
			result.put(market.getName(), new ArrayList<>());
		}
		return result;
	}

	private void reportPositions(Time time) {
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

	private void closePending(TimeMarketData market, int eventIndex) {
		List<OpenTrade> trades = openTrades.get(market.getMarketName());
		Iterator<OpenTrade> iterator = trades.iterator();
		while (iterator.hasNext()) {
			OpenTrade trade = iterator.next();
			if (trade.getTradeStatus() == PENDING_CLOSE) {
				iterator.remove();
				float closePrice = (float) (market.getData(OPEN, 0) * (1.0 - commissionRate));
				recordClose(market.getMarketName(), trade, market.getTime(), eventIndex, closePrice);
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

	private static void processOpenTrades(List<OpenTrade> trades, TimeMarketData market,
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

	private void checkNewSignals(Supplier<SignalGenerator> signalGeneratorFactory,
								 TimeMarketData market, TimeMarketDataSet marketDataSet) {
		List<OpenTrade> trades = openTrades.get(market.getMarketName());
		if (!trades.isEmpty() && !tradeSecondarySignals) {
			return;
		}
		SignalGenerator signalGenerator = requireNonNull(signalGeneratorFactory.get(),
				"signalGeneratorFactory result");
		signalGenerator.shouldBuy(market, marketDataSet).ifPresent(buySignal -> trades.add(
				new OpenTrade(signalGenerator, buySignal, market.getTime())));
	}

	private void recordClose(MarketName market, OpenTrade trade, Time closeTime,
							 int closeEventIndex, float closePrice) {
		opportunities.add(new TradeOpportunity(
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
}

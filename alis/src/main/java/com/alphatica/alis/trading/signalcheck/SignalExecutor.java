package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreGenerator;
import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_CLOSE;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_OPEN;
import static java.lang.String.format;

public class SignalExecutor {
    private final Supplier<TradeSignal> signalSupplier;
    private final Time startTime;
    private final Time endTime;
    private final MarketData marketData;
    private final Predicate<TimeMarketData> marketFilter;
    private final float commissionRate;
    private final boolean tradeSecondarySignals;
    private final ScoreGenerator scoreGenerator;
    private final Map<MarketName, List<OpenTrade>> openTradeMap = new ConcurrentHashMap<>(1024);
    private final AtomicInteger currentlyOpened = new AtomicInteger(0);

    private int maxOpenedPositions = Integer.MAX_VALUE;
    private boolean verbose = false;

    public SignalExecutor(Supplier<TradeSignal> signalSupplier, Time startTime, Time endTime, MarketData marketData,
                          Predicate<TimeMarketData> marketFilter, float commissionRate, boolean tradeSecondarySignals,
                          ScoreGenerator scoreGenerator) {
        this.signalSupplier = signalSupplier;
        this.startTime = startTime;
        this.endTime = endTime;
        this.marketData = marketData;
        this.marketFilter = marketFilter;
        this.commissionRate = commissionRate;
        this.tradeSecondarySignals = tradeSecondarySignals;
        this.scoreGenerator = scoreGenerator;
    }

    public double execute() {
        populateOpenTradesMap();
        List<Time> times = marketData.getTimes().stream().filter(t -> !t.isBefore(startTime) && !t.isAfter(endTime)).toList();
        for (Time time : times) {
            checkTime(time);
        }
        closeLastTrades();
        scoreGenerator.onDone();
        return scoreGenerator.score();
    }

    public SignalExecutor withMaxOpenedPositions(int newMax) {
        maxOpenedPositions = newMax;
        return this;
    }

    public SignalExecutor withVerbose(boolean newVerbose) {
        verbose = newVerbose;
        return this;
    }

    private void populateOpenTradesMap() {
        for(Market market: marketData.listMarkets(ALL)) {
            openTradeMap.put(market.getName(), new ArrayList<>(1024));
        }
    }

    private void closeLastTrades() {
        for (Map.Entry<MarketName, List<OpenTrade>> entry : openTradeMap.entrySet()) {
            for (OpenTrade trade : entry.getValue()) {
                if (trade.getTradeStatus() == TradeStatus.OPEN) {
                    var closePrice = trade.getLastKnowPrice() * (1 - commissionRate);
                    closeTrade(entry.getKey(), trade, closePrice);
                }
            }
        }
    }

    private void checkTime(Time time) {
        TimeMarketDataSet marketDataSet = TimeMarketDataSet.build(time, marketData);
        log(() -> format("%s =================================================", time));
		scoreGenerator.beforeTime(marketDataSet, openTradeMap);
        try (ExecutorService es = Executors.newVirtualThreadPerTaskExecutor()) {
            for (TimeMarketData market : marketDataSet.listMarkets(marketFilter)) {
                es.submit(() -> checkMarketOnTime(market, marketDataSet));
            }
        }
        scoreGenerator.afterTime(marketDataSet, openTradeMap);
        log(() -> format("Opened positions: %d", currentlyOpened.get()));
    }

    private void checkMarketOnTime(TimeMarketData market, TimeMarketDataSet marketDataSet) {
        var openedTrades = openTradeMap.get(market.getMarketName());
        closePending(market, openedTrades);
        openPending(market, openedTrades);
        clearNotOpened(openedTrades);
        processOpenTrades(market, marketDataSet, openedTrades);
        checkNewSignals(market, marketDataSet, openedTrades);
    }

    private void clearNotOpened(List<OpenTrade> openedTrades) {
		openedTrades.removeIf(trade -> trade.getTradeStatus() != TradeStatus.OPEN);
    }

    private void closePending(TimeMarketData market, List<OpenTrade> openedTrades) {
        Iterator<OpenTrade> iterator = openedTrades.iterator();
        while (iterator.hasNext()) {
            var trade = iterator.next();
            if (trade.getTradeStatus() == PENDING_CLOSE) {
                iterator.remove();
                closeTrade(market.getMarketName(), trade, market.getData(OPEN, 0) * (1 - commissionRate));
            }
        }
    }

    private void openPending(TimeMarketData market, List<OpenTrade> openedTrades) {
        var openPrice = market.getData(OPEN, 0) * (1 + commissionRate);
		for (OpenTrade trade : openedTrades) {
			if (trade.getTradeStatus() == PENDING_OPEN) {
                if (currentlyOpened.incrementAndGet() > maxOpenedPositions) {
                    currentlyOpened.decrementAndGet();
                    return;
                }
				trade.setOpenPrice(openPrice);
                log(() -> format("Opening %s at %2f size %.1f", market.getMarketName(), openPrice, trade.getPositionSize()));
			}
		}
    }

    private void closeTrade(MarketName market, OpenTrade trade, float closePrice) {
        scoreGenerator.afterTrade(trade, closePrice);
        currentlyOpened.decrementAndGet();
        log(() -> format("Closing %s at %.2f bought at %.2f profit %.2f",  market, closePrice, trade.getOpenPrice(), percentChange(trade.getOpenPrice(), closePrice)));
    }

    private void processOpenTrades(TimeMarketData market, TimeMarketDataSet marketDataSet, List<OpenTrade> openTrades) {
        for (OpenTrade openTrade : openTrades) {
            if (openTrade.getTradeStatus() != TradeStatus.OPEN) {
                continue;
            }
            openTrade.updateLastKnownPrice(market.getData(CLOSE, 0));
            openTrade.incrementBars();
            var tradeSignal = openTrade.getSignal();
            tradeSignal.afterClose(market, marketDataSet);
            if (tradeSignal.shouldSell(market, marketDataSet)) {
                openTrade.setStatus(PENDING_CLOSE);
            }
        }
    }

    private void checkNewSignals(TimeMarketData market, TimeMarketDataSet marketDataSet, List<OpenTrade> openTrades) {
        if (openTrades.isEmpty() || tradeSecondarySignals) {
            var tradeSignal = signalSupplier.get();
            var newPosition = tradeSignal.shouldBuy(market, marketDataSet);
            if (newPosition > 0) {
                openTrades.add(new OpenTrade(tradeSignal, newPosition));
            }
        }
    }

    private void log(Supplier<String> message) {
        if (verbose) {
            System.out.println(message.get());
        }
    }
}

package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.signalcheck.scoregenerator.CompoundProfitAfterReductionScoreGenerator;
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
import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public class SignalExecutor {
    private final AtomicInteger trades = new AtomicInteger(0);
    private final AtomicInteger profit = new AtomicInteger(0);
    private final AtomicInteger bars = new AtomicInteger(0);
    private final Supplier<TradeSignal> signalSupplier;
    private final Time startTime;
    private final Time endTime;
    private final MarketData marketData;
    private final Predicate<TimeMarketData> marketFilter;
    private final float commissionRate;
    private final boolean tradeSecondarySignals;
    private final List<Double> tradesReturns = Collections.synchronizedList(new ArrayList<>());
    private final List<ScoreGenerator> scoreGenerators = List.of(new CompoundProfitAfterReductionScoreGenerator());

    private final Map<MarketName, PendingOpen> pendingOpens = new ConcurrentHashMap<>(1024);
    private final Map<MarketName, PendingClose> pendingCloses = new ConcurrentHashMap<>(1024);
    private final Map<MarketName, List<OpenTrade>> openTradeMap = new ConcurrentHashMap<>(1024);
    private double sumOfLogReturns = 0;

    public SignalExecutor(Supplier<TradeSignal> signalSupplier, Time startTime, Time endTime, MarketData marketData,
                          Predicate<TimeMarketData> marketFilter, float commissionRate, boolean tradeSecondarySignals) {
        this.signalSupplier = signalSupplier;
        this.startTime = startTime;
        this.endTime = endTime;
        this.marketData = marketData;
        this.marketFilter = marketFilter;
        this.commissionRate = commissionRate;
        this.tradeSecondarySignals = tradeSecondarySignals;
    }

    public void execute() {
        long executionStartTime = System.nanoTime();
        List<Time> times = marketData.getTimes().stream().filter(t -> !t.isBefore(startTime) && !t.isAfter(endTime)).toList();
        for (Time time : times) {
            checkTime(time);
        }
        closeLastTrades();
        System.out.println("Trades: " + trades.get());
        System.out.println("Profit: " + profit.get());
        System.out.println("Bars: " + bars.get());
        System.out.println("Profit per bar: " + (double) profit.get() / bars.get());
        System.out.println("Profit per trade: " + (double) profit.get() / trades.get());
        var barsPerTrade = (double) bars.get() / trades.get();
        System.out.println("Bars per trade: " + barsPerTrade);
        var geometricMeanTradeReturn = Math.exp(sumOfLogReturns / trades.get());
        System.out.println("Geometric mean trade return: " + geometricMeanTradeReturn);
        scoreGenerators.forEach(ScoreGenerator::onDone);
        long executionEndTime = System.nanoTime();
        System.out.printf("Execution time: %d%n", (executionEndTime - executionStartTime) / 1_000_000);
    }

    private void closeLastTrades() {
        for (Map.Entry<MarketName, List<OpenTrade>> entry : openTradeMap.entrySet()) {
            for (OpenTrade trade : entry.getValue()) {
                var closePrice = trade.getLastKnowPrice();
                closeTrade(trade, closePrice);
//              System.out.println("C: (end) " + entry.getKey() + " " + percentChange(trade.getOpenPrice(), closePrice));
            }
        }
    }

    private void checkTime(Time time) {
        TimeMarketDataSet marketDataSet = TimeMarketDataSet.build(time, marketData);
        try (ExecutorService es = Executors.newVirtualThreadPerTaskExecutor()) {
            for (TimeMarketData market : marketDataSet.listMarkets(marketFilter)) {
                es.submit(() -> checkMarketOnTime(market, marketDataSet));
            }
        }
    }

    private void checkMarketOnTime(TimeMarketData market, TimeMarketDataSet marketDataSet) {
        openPending(market);
        closePending(market);
        var openedTrades = getOpenTrades(market);
        processOpenTrades(market, marketDataSet, openedTrades);
        checkNewSignals(market, marketDataSet, openedTrades);
    }

    private void closePending(TimeMarketData market) {
        var pending = pendingCloses.remove(market.getMarketName());
        if (pending == null) {
            return;
        }
        Iterator<OpenTrade> iterator = openTradeMap.get(market.getMarketName()).iterator();
        while (iterator.hasNext()) {
            var trade = iterator.next();
            if (trade.getSignal().equals(pending.tradeSignal().getSignal())) {
                iterator.remove();
                closeTrade(trade, market.getData(OPEN, 0));
                // System.out.println("C: " + market.getTime() + " " + market.getMarketName() + " " + percentChange(trade.getOpenPrice(), closePrice));
            }
        }
    }

    private void openPending(TimeMarketData market) {
        var pending = pendingOpens.remove(market.getMarketName());
        if (pending == null) {
            return;
        }
//        System.out.println("O: " + market.getTime() + " " + market.getMarketName());
        trades.incrementAndGet();
        var openPrice = market.getData(OPEN, 0) * (1 + commissionRate);
        openTradeMap.get(market.getMarketName())
                .add(new OpenTrade(pending.signal(), openPrice, pending.position()));
    }

    private void closeTrade(OpenTrade trade, float closePrice) {
        var effectivePrice = closePrice * (1 - commissionRate);
        bars.addAndGet(trade.getBars());
        var tradeReturn = ((effectivePrice / trade.getOpenPrice() - 1) * trade.getPosition()) + 1;
        sumOfLogReturns += Math.log(tradeReturn);
        tradesReturns.add((double)(effectivePrice / trade.getOpenPrice() - 1) * trade.getPosition());
        profit.addAndGet((int) (Math.round(percentChange(trade.getOpenPrice(), effectivePrice) * trade.getPosition())));
        scoreGenerators.forEach(s -> s.afterTrade(trade, effectivePrice));
    }

    private void processOpenTrades(TimeMarketData market, TimeMarketDataSet marketDataSet, List<OpenTrade> openTrades) {
        for (OpenTrade openTrade : openTrades) {
            openTrade.updateLastKnownPrice(market.getData(CLOSE, 0));
            openTrade.incrementBars();
            var tradeSignal = openTrade.getSignal();
            tradeSignal.afterClose(market, marketDataSet);
            if (tradeSignal.shouldSell(market, marketDataSet)) {
                submitClose(openTrade, market.getMarketName());
            }
        }
    }

    private void checkNewSignals(TimeMarketData market, TimeMarketDataSet marketDataSet, List<OpenTrade> openTrades) {
        if (openTrades.isEmpty() || tradeSecondarySignals) {
            var tradeSignal = signalSupplier.get();
            var nextPosition = tradeSignal.shouldBuy(market, marketDataSet);
            if (nextPosition > 0) {
                submitOpen(tradeSignal, market.getMarketName(), nextPosition);
            }
        }
    }

    private List<OpenTrade> getOpenTrades(TimeMarketData market) {
        var marketTrades = openTradeMap.get(market.getMarketName());
        if (marketTrades == null) {
            marketTrades = openTradeMap.put(market.getMarketName(), new ArrayList<>(1024));
        }
        return marketTrades;
    }

    private void submitOpen(TradeSignal tradeSignal, MarketName market, float nextPosition) {
        pendingOpens.put(market, new PendingOpen(tradeSignal, nextPosition));
    }

    private void submitClose(OpenTrade tradeSignal, MarketName market) {
        pendingCloses.put(market, new PendingClose(tradeSignal));
    }

}

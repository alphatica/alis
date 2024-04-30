package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Position;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;


public class StrategyExecutor {
    private double commission = 0.01;
    private double initialCash = 100_000.0;
    private Time timeFrom = new Time(0);
    private Time timeTo = new Time(Integer.MAX_VALUE);
    private Double limitOrderSize = Double.NaN;
    private TradePrice tradePrice = TradePrice.OPEN;

    public StrategyExecutor withInitialCash(double initialCash) {
        this.initialCash = initialCash;
        return this;
    }

    public StrategyExecutor withCommission(double commission) {
        this.commission = commission;
        return this;
    }

    public StrategyExecutor withTimeRange(Time timeFrom, Time timeTo) {
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        return this;
    }

    public StrategyExecutor withTradePrice(TradePrice tradePrice) {
        this.tradePrice = tradePrice;
        return this;
    }

    public StrategyExecutor withLimitOrderSize(Double limitOrderSize) {
        this.limitOrderSize = limitOrderSize;
        return this;
    }

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public Account execute(MarketData marketData, Strategy strategy) {
        long startTime = System.currentTimeMillis();
        List<Time> times = marketData.getTimes().stream().filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo)).toList();
        List<Order> pendingOrders = new ArrayList<>();
        Account account = new Account(initialCash);
        TimeMarketDataSet current = null;
        for (Time time : times) {
            current = TimeMarketDataSet.build(time, marketData);
            executeSells(pendingOrders, current, account);
            executeBuys(pendingOrders, current, account);
            account.updateLastKnown(current);
            pendingOrders = strategy.afterClose(current, account);
            Collections.sort(pendingOrders);
            Collections.reverse(pendingOrders);
        }
        if (current != null) {
            closeAccount(account, current);
        }
        strategy.finished(account);
        long endTime = System.currentTimeMillis();
        System.out.println("Strategy executed in " + (endTime - startTime) + " ms");
        return account;
    }

    @SuppressWarnings("java:S1301")
    private double getRequestedCount(Order order, Account account, double price) {
        switch (order.size()) {
            case PROPORTION -> {
                switch (order.direction()) {
                    case BUY -> {
                        // Multiply value by 0.9999 to avoid "Not enough cash to buy" due to double precision issues.
                        return order.sizeValue() * account.getNAV() * 0.9999 / price;
                    }
                    case SELL -> {
                        return order.sizeValue() * account.getPosition(order.market()).map(Position::getSize).orElse(0.0);
                    }
                }
            }
            case COUNT -> {
                return order.sizeValue();
            }
        }
        throw new AssertionError("Not all OrderSize variants have been processed");
    }

    private void closeAccount(Account account, TimeMarketDataSet last) {
        for (TimeMarketData market : last.listMarkets()) {
            account.getPosition(market.getMarketName()).ifPresent(p -> {
                double value = p.getSize() * p.getLastPrice() * (1 - commission);
                account.addTrade(market.getMarketName(), -p.getSize());
                account.addCash(value);
            });
        }
    }

    private void executeBuys(List<Order> pendingOrders, TimeMarketDataSet current, Account account) {
        for (Order order : pendingOrders) {
            if (order.direction() == BUY) {
                TimeMarketData marketData = current.get(order.market());
                if (marketData != null) {
                    double price = tradePrice.getPrice(marketData) * (1 + commission);
                    double possibleCount = getPossibleCount(getRequestedCount(order, account, price), marketData);
                    double value = possibleCount * price;
                    if (value <= account.getCash()) {
                        account.addTrade(order.market(), possibleCount);
                        account.addCash(-value);
                    }
                }
            }
        }
    }

    private void executeSells(List<Order> pendingOrders, TimeMarketDataSet current, Account account) {
        for (Order order : pendingOrders) {
            if (order.direction() == SELL) {
                TimeMarketData marketData = current.get(order.market());
                if (marketData != null) {
                    double price = tradePrice.getPrice(marketData);
                    double possibleCount = getPossibleCount(getRequestedCount(order, account, price), marketData);
                    double value = possibleCount * price * (1 - commission);
                    account.addTrade(order.market(), -possibleCount);
                    account.addCash(value);
                }
            }
        }
    }

    private double getPossibleCount(double count, TimeMarketData marketData) {
        if (!limitOrderSize.isNaN()) {
            double traded = marketData.getData(TURNOVER, 0) / marketData.getAveragePrice(0);
            return Math.min(traded * 0.2, count);
        } else {
            return count;
        }

    }

}

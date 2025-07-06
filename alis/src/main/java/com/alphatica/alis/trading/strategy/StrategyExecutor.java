package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;

public class StrategyExecutor {

	private double commissionRate = 0.01;
	private double initialCash = 100_000.0;
	private Time timeFrom = new Time(0);
	private Time timeTo = new Time(Integer.MAX_VALUE);
	private Double limitOrderSize = Double.NaN;
	private TradePrice tradePrice = TradePrice.OPEN;
	private double skipTradesProbability = 0.0;
	private int missedTrades = 0;
	private BarExecutedConsumer barExecutedConsumer = (time, account, pendingOrders) -> {};

	public StrategyExecutor withInitialCash(double initialCash) {
		this.initialCash = initialCash;
		return this;
	}

	public StrategyExecutor withCommissionRate(double commissionRate) {
		this.commissionRate = commissionRate;
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

	public StrategyExecutor withBarExecutedConsumer(BarExecutedConsumer barExecutedConsumer) {
		this.barExecutedConsumer = barExecutedConsumer;
		return this;
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public Account execute(MarketData marketData, Strategy strategy) throws AccountActionException {
		List<Time> times = marketData.getTimes().stream().filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo)).toList();
		Account account = new Account(initialCash);
		if (times.isEmpty()) {
			return account;
		}
		List<Order> pendingOrders = new ArrayList<>();
		account.getAccountHistory().addAction(new AccountAction(times.getFirst(), new Deposit(initialCash)));
		for (Time time : times) {
			TimeMarketDataSet current = TimeMarketDataSet.build(time, marketData);
			executeSells(pendingOrders, current, account);
			executeBuys(pendingOrders, current, account);
			updateMissedTradesCounter(pendingOrders);
			account.updateLastKnown(current);
			pendingOrders = strategy.afterClose(current, account);
			if (skipTradesProbability > 0.0) {
				pendingOrders.removeIf(o -> ThreadLocalRandom.current().nextDouble() < skipTradesProbability);
			}
			Collections.sort(pendingOrders);
			Collections.reverse(pendingOrders);
			barExecutedConsumer.execute(time, account, pendingOrders);
		}
		account.close(commissionRate);
		strategy.finished(account);
		return account;
	}

	private void updateMissedTradesCounter(List<Order> pendingOrders) {
		for(Order order: pendingOrders) {
			if (order.direction() == BUY) {
				missedTrades++;
			}
		}
	}

	public void skipTrades(double skipTradesProbability) {
		this.skipTradesProbability = skipTradesProbability;
	}

	public int getMissedTrades() {
		return missedTrades;
	}

	@SuppressWarnings("java:S1301")
	private int getRequestedCount(Order order, Account account, double price) {
		switch (order.size()) {
			case PERCENTAGE -> {
				switch (order.direction()) {
					case BUY -> {
						return (int) Math.floor(order.sizeValue() * (account.getNAV() / price) / 100);
					}
					case SELL -> {
						return order.sizeValue() * account.getPosition(order.market()).getQuantity() / 100;
					}
				}
			}
			case COUNT -> {
				return order.sizeValue();
			}
		}
		throw new AssertionError("Not all OrderSize variants have been processed");
	}

	private void executeBuys(List<Order> pendingOrders, TimeMarketDataSet current, Account account) throws AccountActionException {
		Iterator<Order> orderIterator = pendingOrders.iterator();
		while(orderIterator.hasNext()) {
			Order order = orderIterator.next();
			if (order.direction() == BUY) {
				TimeMarketData marketData = current.get(order.market());
				if (marketData != null) {
					double price = tradePrice.getPrice(marketData);
					int quantity = getPossibleCount(getRequestedCount(order, account, price), marketData);
					if (quantity == 0) {
						continue;
					}
					double commissionValue = quantity * price * commissionRate;
					double value = quantity * price + commissionValue;
					if (value > account.getCash()) {
						return;
					}
					orderIterator.remove();
					account.addPosition(order.market(), new PositionEntry(current.getTime(), quantity, tradePrice.getPrice(marketData)),
							commissionValue);
					account.getAccountHistory()
						   .addAction(new AccountAction(current.getTime(), new Trade(marketData.getMarketName(), BUY, price, quantity,
								   commissionValue)));
				}
			}
		}
	}

	private void executeSells(List<Order> pendingOrders, TimeMarketDataSet current, Account account) throws AccountActionException {
		Iterator<Order> orderIterator = pendingOrders.iterator();
		while(orderIterator.hasNext()) {
			Order order = orderIterator.next();
			if (order.direction() == SELL) {
				TimeMarketData marketData = current.get(order.market());
				if (marketData != null) {
					double price = tradePrice.getPrice(marketData);
					int quantity = getPossibleCount(getRequestedCount(order, account, price), marketData);
					if (quantity > 0) {
						double commissionValue = quantity * price * commissionRate;
						PositionExit exit = new PositionExit(current.getTime(), quantity, price);
						account.reducePosition(order.market(), exit, commissionValue);
						account.getAccountHistory()
							   .addAction(new AccountAction(current.getTime(), new Trade(marketData.getMarketName(), SELL, price, quantity,
									   commissionValue)));
						orderIterator.remove();
					}
				}
			}
		}
	}

	private int getPossibleCount(int count, TimeMarketData marketData) {
		if (!limitOrderSize.isNaN()) {
			double traded = marketData.getData(TURNOVER, 0) / marketData.getAveragePrice(0);
			return (int) Math.min(traded * limitOrderSize, count);
		} else {
			return count;
		}
	}
}

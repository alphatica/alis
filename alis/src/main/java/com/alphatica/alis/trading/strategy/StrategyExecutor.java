package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alphatica.alis.trading.order.Direction.BUY;

public class StrategyExecutor {

	private final AtomicBoolean executed = new AtomicBoolean(false);
	private double commissionRate = 0.01;
	private double initialCash = 100_000.0;
	private Time timeFrom = new Time(0);
	private Time timeTo = new Time(Integer.MAX_VALUE);
	private Double limitOrderSize = Double.NaN;
	private TradePrice tradePrice = TradePrice.OPEN;
	private double skipTradesProbability = 0.0;
	private int missedTrades = 0;
	private BarExecutedConsumer barExecutedConsumer = (time, account, pendingOrders) -> {
	};
	private boolean verbose = false;
	private boolean useCachedMarketData = false;

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

	public StrategyExecutor withTimeFrom(Time timeFrom) {
		this.timeFrom = timeFrom;
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

	public StrategyExecutor withVerbose(boolean value) {
		this.verbose = value;
		return this;
	}

	public StrategyExecutor useCachedMarketData() {
		this.useCachedMarketData = true;
		return this;
	}

	public Time getTimeFrom() {
		return timeFrom;
	}

	public Account execute(MarketData marketData, Strategy strategy) throws AccountActionException {
		ensureNotExecutedBefore();
		StrategyExecutionLogger logger = new StrategyExecutionLogger(verbose);
		StrategyTradeExecutor tradeExecutor = new StrategyTradeExecutor(commissionRate, tradePrice, limitOrderSize, logger);
		List<Time> times = marketData.getTimes().stream().filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo)).toList();
		Account account = new Account(initialCash);
		if (times.isEmpty()) {
			logger.log("No quotes in requested time range. Quitting.");
			return account;
		}
		List<Order> pendingOrders = new ArrayList<>();
		account.getAccountHistory().addAction(new AccountAction(times.getFirst(), new Deposit(initialCash)));
		for (Time time : times) {
			logger.log("____________________________________________________________________________");
			logger.log("Starting time: %s", time);
			TimeMarketDataSet current = getTimeMarketDataSet(marketData, time);
			logger.log("Pending orders: %d", pendingOrders.size());
			tradeExecutor.executeSells(pendingOrders, current, account);
			logger.log("Finished selling. Cash available: %.2f", account.getCash());
			account.afterSells();
			tradeExecutor.executeBuys(pendingOrders, current, account);
			logger.log("Finished buying. Cash left: %.2f", account.getCash());
			updateMissedTradesCounter(pendingOrders);
			account.updateLastKnown(current);
			pendingOrders = getNewPendingOrders(strategy, current, account, logger);
			barExecutedConsumer.execute(time, account, pendingOrders);
			logger.showPositions(account);
			logger.log("Finished time %s: Net asset value: %.2f Cash: %.2f Drawdown: %.2f Downside drawdown: %.2f",
					time, account.getNAV(), account.getCash(), account.getCurrentDD(), account.getCurrentDownsideDD()
			);
		}
		account.close(commissionRate);
		logger.log("Account closed with NAV: %.2f", account.getNAV());
		strategy.finished(account);
		return account;
	}

	private void ensureNotExecutedBefore() {
		if (!executed.compareAndSet(false, true)) {
			throw new IllegalStateException("StrategyExecutor can only be executed once");
		}
	}

	private TimeMarketDataSet getTimeMarketDataSet(MarketData marketData, Time time) {
		if (useCachedMarketData) {
			return marketData.cachedSnapshotAt(time);
		} else {
			return marketData.snapshotAt(time);
		}
	}

	private List<Order> getNewPendingOrders(Strategy strategy, TimeMarketDataSet current, Account account,
											StrategyExecutionLogger logger) {
		List<Order> pendingOrders = new ArrayList<>(strategy.afterClose(current, account));
		if (skipTradesProbability > 0.0) {
			pendingOrders.removeIf(o -> ThreadLocalRandom.current().nextDouble() < skipTradesProbability);
		}
		Collections.sort(pendingOrders);
		Collections.reverse(pendingOrders);
		for(var order: pendingOrders) {
			logger.log("New pending order: %s %s. Size: %d%s. Priority: %.2f",
					order.direction(), order.market(), order.sizeValue(), order.size().shortSign(), order.priority());
		}
		return pendingOrders;
	}

	private void updateMissedTradesCounter(List<Order> pendingOrders) {
		for (Order order : pendingOrders) {
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

}

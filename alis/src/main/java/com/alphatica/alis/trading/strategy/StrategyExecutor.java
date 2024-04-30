package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Position;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static com.alphatica.alis.trading.order.Direction.LONG;
import static com.alphatica.alis.trading.order.Direction.EXIT;


public class StrategyExecutor {
	private double commission = 0.01;
	private double initialCash = 100_000.0;
	private Time timeFrom = new Time(0);
	private Time timeTo = new Time(Integer.MAX_VALUE);
	private Double limitOrderSize = Double.NaN;
	private TradePrice tradePrice = TradePrice.OPEN;
	private LineChartData<Time> benchmarkLine = new LineChartData<>();
	private LineChartData<Time> equityLine = new LineChartData<>();

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
		List<Time> times = marketData.getTimes()
									 .stream()
									 .filter(time -> !time.isBefore(timeFrom) && !time.isAfter(timeTo))
									 .toList();
		List<Order> pendingOrders = new ArrayList<>();
		Account account = new Account(initialCash, commission);
		TimeMarketDataSet current = null;
		equityLine.setConnectPoints(true);
		benchmarkLine.setConnectPoints(true);
		for (Time time : times) {
			current = TimeMarketDataSet.build(time, marketData);
			TimeMarketData benchmark = current.get(new MarketName("wig"));
			if (benchmark != null) {
				equityLine.addPoint(time, account.getNAV());
				benchmarkLine.addPoint(time, benchmark.getData(CLOSE, 0));
			}
			executeSells(pendingOrders, current, account);
			executeBuys(pendingOrders, current, account);
			account.updateLastKnown(current);
			pendingOrders = strategy.afterClose(current, account);
			Collections.sort(pendingOrders);
			Collections.reverse(pendingOrders);
		}
		if (current != null) {
			closeAccount(account);
		}
		strategy.finished(account);
		return account;
	}

	public LineChartData<Time> getBenchmarkLine() {
		return benchmarkLine;
	}

	public LineChartData<Time> getEquityLine() {
		return equityLine;
	}

	@SuppressWarnings("java:S1301")
	private double getRequestedCount(Order order, Account account, double price) {
		switch (order.size()) {
			case PROPORTION -> {
				switch (order.direction()) {
					case LONG -> {
						// Multiply ratio by 0.9999 to avoid "Not enough cash to buy" due to double precision issues.
						return order.sizeValue() * account.getNAV() * 0.9999 / price;
					}
					case EXIT -> {
						return order.sizeValue() * account.getPosition(order.market()).getQuantity();
					}
				}
			}
			case COUNT -> {
				return order.sizeValue();
			}
		}
		throw new AssertionError("Not all OrderSize variants have been processed");
	}

	private void closeAccount(Account account) {
		for (Map.Entry<MarketName, Position> next : account.getPositions().entrySet()) {
			PositionExit exit = new PositionExit(next.getValue().getQuantity(), next.getValue().getLastPrice());
			account.reducePosition(next.getKey(), exit);
		}
	}

	private void executeBuys(List<Order> pendingOrders, TimeMarketDataSet current, Account account) {
		for (Order order : pendingOrders) {
			if (order.direction() == LONG) {
				TimeMarketData marketData = current.get(order.market());
				if (marketData != null) {
					double price = tradePrice.getPrice(marketData) * (1 + commission);
					double possibleCount = getPossibleCount(getRequestedCount(order, account, price), marketData);
					double value = possibleCount * price;
					if (value <= account.getCash()) {
						account.addPosition(order.market(), tradePrice.getPrice(marketData), possibleCount);
					} else {
						break;
					}
				}
			}
		}
	}

	private void executeSells(List<Order> pendingOrders, TimeMarketDataSet current, Account account) {
		for (Order order : pendingOrders) {
			if (order.direction() == EXIT) {
				TimeMarketData marketData = current.get(order.market());
				if (marketData != null) {
					double price = tradePrice.getPrice(marketData);
					double possibleCount = getPossibleCount(getRequestedCount(order, account, price), marketData);
					PositionExit exit = new PositionExit(possibleCount, tradePrice.getPrice(marketData));
					account.reducePosition(order.market(), exit);
				}
			}
		}
	}

	private double getPossibleCount(double count, TimeMarketData marketData) {
		if (!limitOrderSize.isNaN()) {
			double traded = marketData.getData(TURNOVER, 0) / marketData.getAveragePrice(0);
			return Math.min(traded * limitOrderSize, count);
		} else {
			return count;
		}
	}

}

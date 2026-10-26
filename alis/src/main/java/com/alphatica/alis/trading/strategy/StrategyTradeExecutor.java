package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.account.Quantities;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;

import java.util.Iterator;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;

final class StrategyTradeExecutor {

	private final double commissionRate;
	private final TradePrice tradePrice;
	private final Double limitOrderSize;
	private final StrategyExecutionLogger logger;

	StrategyTradeExecutor(double commissionRate, TradePrice tradePrice, Double limitOrderSize,
						  StrategyExecutionLogger logger) {
		this.commissionRate = commissionRate;
		this.tradePrice = tradePrice;
		this.limitOrderSize = limitOrderSize;
		this.logger = logger;
	}

	void executeBuys(List<Order> pendingOrders, TimeMarketDataSet current, Account account)
			throws AccountActionException {
		Iterator<Order> orders = pendingOrders.iterator();
		while (orders.hasNext()) {
			Order order = orders.next();
			if (order.direction() != BUY) {
				continue;
			}
			TimeMarketData marketData = currentMarket(order, current);
			if (marketData == null) {
				continue;
			}
			BuyResult result = executeBuy(order, marketData, current, account);
			if (result == BuyResult.EXECUTED) {
				orders.remove();
			} else if (result == BuyResult.INSUFFICIENT_CASH) {
				return;
			}
		}
	}

	void executeSells(List<Order> pendingOrders, TimeMarketDataSet current, Account account)
			throws AccountActionException {
		Iterator<Order> orders = pendingOrders.iterator();
		while (orders.hasNext()) {
			Order order = orders.next();
			if (order.direction() != SELL) {
				continue;
			}
			TimeMarketData marketData = currentMarket(order, current);
			if (marketData == null) {
				continue;
			}
			if (executeSell(order, marketData, current, account)) {
				orders.remove();
			}
		}
	}

	private BuyResult executeBuy(Order order, TimeMarketData marketData, TimeMarketDataSet current,
								 Account account) throws AccountActionException {
		double price = tradePrice.getPrice(marketData);
		double quantity = possibleQuantity(requestedQuantity(order, account, price), marketData);
		if (quantity == 0) {
			logger.log("Ignoring buy order for %s. Quantity = 0", order.market());
			return BuyResult.IGNORED;
		}
		double commission = quantity * price * commissionRate;
		double value = quantity * price + commission;
		logger.log("Trying to buy %s x %s at %.2f", marketData.getMarketName(), Quantities.format(quantity), price);
		if (value > account.getCash()) {
			logger.log("Unable to buy. Not enough cash. Required: %.2f available: %.2f", value, account.getCash());
			return BuyResult.INSUFFICIENT_CASH;
		}
		account.addPosition(order.market(), new PositionEntry(current.getTime(), quantity, price), commission);
		recordTrade(account, current, marketData, BUY, price, quantity, commission);
		logger.log("Bought %s x %s", order.market(), Quantities.format(quantity));
		return BuyResult.EXECUTED;
	}

	private boolean executeSell(Order order, TimeMarketData marketData, TimeMarketDataSet current,
								Account account) throws AccountActionException {
		double price = tradePrice.getPrice(marketData);
		double quantity = possibleQuantity(requestedQuantity(order, account, price), marketData);
		if (quantity <= 0) {
			return false;
		}
		var position = account.getPosition(order.market());
		double commission = quantity * price * commissionRate;
		logger.log("Selling %s x %s at %.2f, bought at %.2f, profit: %.1f%% / %.1f",
				order.market(), Quantities.format(quantity), price, position.getEntryPrice(),
				percentChange(position.getEntryPrice(), price),
				quantity * (price - position.getEntryPrice()) - commission);
		account.reducePosition(order.market(), new PositionExit(current.getTime(), quantity, price), commission);
		recordTrade(account, current, marketData, SELL, price, quantity, commission);
		return true;
	}

	private static TimeMarketData currentMarket(Order order, TimeMarketDataSet current) {
		TimeMarketData marketData = current.get(order.market());
		return marketData != null && marketData.getTime().equals(current.getTime()) ? marketData : null;
	}

	/**
	 * Requested quantity, not rounded yet.
	 */
	private double requestedQuantity(Order order, Account account, double price) {
		return switch (order.size()) {
			case PERCENTAGE -> percentageQuantity(order, account, price);
			case COUNT -> order.sizeValue();
			case VALUE -> valueQuantity(order, account, price);
		};
	}

	private double percentageQuantity(Order order, Account account, double price) {
		return switch (order.direction()) {
			case BUY -> order.sizeValue() * account.getNAV() / 100.0 / unitCostIncludingCommission(price);
			case SELL -> order.sizeValue() * account.getPosition(order.market()).getQuantity() / 100;
		};
	}

	private double valueQuantity(Order order, Account account, double price) {
		return switch (order.direction()) {
			case BUY -> order.sizeValue() / unitCostIncludingCommission(price);
			case SELL -> Math.min(order.sizeValue() / price, account.getPosition(order.market()).getQuantity());
		};
	}

	private double unitCostIncludingCommission(double price) {
		return price * (1.0 + commissionRate);
	}

	/**
	 * Requested quantity limited by turnover (if set) and rounded down to the smallest quantity the account can hold.
	 */
	private double possibleQuantity(double quantity, TimeMarketData marketData) {
		if (limitOrderSize.isNaN()) {
			return Quantities.roundDown(quantity);
		}
		double traded = marketData.getData(TURNOVER, 0) / marketData.getAveragePrice(0);
		return Quantities.roundDown(Math.min(traded * limitOrderSize, quantity));
	}

	private static void recordTrade(Account account, TimeMarketDataSet current, TimeMarketData marketData,
									Direction direction, double price, double quantity, double commission)
			throws AccountActionException {
		account.getAccountHistory().addAction(new AccountAction(current.getTime(),
				new Trade(marketData.getMarketName(), direction, price, quantity, commission)));
	}

	private enum BuyResult {
		EXECUTED,
		IGNORED,
		INSUFFICIENT_CASH
	}
}

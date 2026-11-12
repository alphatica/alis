package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Quantities;
import com.alphatica.alis.trading.order.Order;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;

final class OrderQuantities {

	private final double commissionRate;
	private final Double limitOrderSize;

	OrderQuantities(double commissionRate, Double limitOrderSize) {
		this.commissionRate = commissionRate;
		this.limitOrderSize = limitOrderSize;
	}

	double possibleQuantity(Order order, TimeMarketData marketData, Account account, double price) {
		return possibleQuantity(requestedQuantity(order, account, price), marketData);
	}

	double unitCostIncludingCommission(double price) {
		return price * (1.0 + commissionRate);
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
}

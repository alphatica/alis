package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;
import static java.lang.String.format;

public class Account {
	private final Map<MarketName, Position> positions = new HashMap<>();
	private final DrawDownCalc ddCalc;
	private final DownsideDrawdownCalculator downsideDrawdownCalculator;
	private final AccountHistory accountHistory;
	private double cash;

	public Account(double cash) {
		this.downsideDrawdownCalculator = new DownsideDrawdownCalculator(cash);
		this.cash = cash;
		this.ddCalc = new DrawDownCalc();
		accountHistory = new AccountHistory(cash);
	}

	public double getMaxDD() {
		return ddCalc.getMaxDD();
	}

	public double getCurrentDD() {
		return ddCalc.getCurrentDD();
	}

	public double getMaxDownsideDD() {
		return downsideDrawdownCalculator.getMaxDownsideDrawdown();
	}

	public double getCurrentDownsideDD() {
		return downsideDrawdownCalculator.getCurrentDownsideDrawdown();
	}

	public Position getPosition(MarketName market) {
		return positions.get(market);
	}

	public double getPositionValue(MarketName marketName) {
		Position position = positions.get(marketName);
		if (position != null) {
			return position.getQuantity() * position.getEntryPrice();
		} else {
			return 0;
		}
	}

	public Map<MarketName, Position> getPositions() {
		return new HashMap<>(positions);
	}

	public void addPosition(MarketName market, PositionEntry entry, double commission) throws AccountActionException {
		double value = entry.quantity * entry.price;
		double cashNeeded = value + commission;
		if (cashNeeded > cash) {
			throw new AccountActionException(format("Not enough cash to buy %s on %s. Missing cash: %.2f", market, entry.time, cash - cashNeeded));
		}
		Position position = positions.get(market);
		if (position != null) {
			position.add(new PositionEntry(entry.time, entry.quantity, entry.price));
		} else {
			positions.put(market, new Position(market, new PositionEntry(entry.time, entry.quantity, entry.price)));
		}
		accountHistory.addCommission(commission);
		cash -= cashNeeded;
	}

	public void reducePosition(MarketName market, PositionExit exit, double commission) throws AccountActionException {
		Position position = positions.get(market);
		if (position == null || position.getQuantity() < exit.quantity) {
			throw new AccountActionException(format("Unable to sell %d of %s on %s", exit.quantity, market, exit.time));
		}
		List<PositionPricesRecord> positionPricesRecords = position.reduce(exit);
		accountHistory.addRecords(positionPricesRecords);
		accountHistory.addCommission(commission);
		double value = exit.price * exit.quantity;
		double cashReceived = value - commission;
		cash += cashReceived;
		if (position.getQuantity() == 0) {
			Position removed = positions.remove(market);
			addToHistory(market, removed);
		}
	}

	public void addCash(double cash) {
		assertPositive(cash);
		this.cash += cash;
		ddCalc.changeCash(cash);
		accountHistory.recordCashPayment(cash);
	}

	public void reduceCash(double cash) throws AccountActionException {
		assertPositive(cash);
		if (cash > this.cash) {
			throw new AccountActionException(format("Unable to withdraw %.2f, only %.2f available", cash, this.cash));
		}
		this.cash -= cash;
		ddCalc.changeCash(-cash);
		accountHistory.recordCashPayment(-cash);
	}

	public double getCash() {
		return cash;
	}

	public double getNAV() {
		double nav = cash;
		for (Position position : positions.values()) {
			nav += position.getLastClose() * position.getQuantity();
		}
		return nav;
	}

	public double calcCashProfit() {
		return getNAV() - accountHistory.getCashPayments();
	}

	public void updateLastKnown(TimeMarketDataSet data) {

		positions.forEach((market, position) -> {
			TimeMarketData marketData = data.get(market);
			if (marketData != null) {
				position.updatePrices(marketData.getData(Layer.CLOSE, 0), marketData.getData(Layer.HIGH, 0), marketData.getData(Layer.LOW, 0));
			}
		});
		var nav = getNAV();
		ddCalc.updateNav(nav);
		downsideDrawdownCalculator.updateState(cash, nav - cash);
	}

	public AccountHistory getAccountHistory() {
		return accountHistory;
	}

	public List<PositionPricesRecord> getClosedPricesRecords() {
		return accountHistory.getPricesRecords();
	}

	public List<PositionPricesRecord> getAllPricesRecords() {
		ArrayList<PositionPricesRecord> records = new ArrayList<>(accountHistory.getPricesRecords());
		records.addAll(positions.values().stream().map(Position::getOpenPositionsPricesRecords).flatMap(Collection::stream).toList());
		return records;
	}

	public void close(double commissionRate) throws AccountActionException {
		for (Map.Entry<MarketName, Position> next : getPositions().entrySet()) {
			double price = next.getValue().getLastClose();
			int quantity = next.getValue().getQuantity();
			PositionExit exit = new PositionExit(null, quantity, price);
			double commissionValue = quantity * price * commissionRate;
			reducePosition(next.getKey(), exit, commissionValue);
		}
		downsideDrawdownCalculator.updateState(cash, 0);
	}

	public void afterSells() {
		downsideDrawdownCalculator.updateState(cash, getNAV() - cash);
	}

	private void addToHistory(MarketName marketName, Position removed) {
		PositionStats stats = removed.getStats();
		PositionResult positionResult = new PositionResult(stats.profitValue(), stats.profitPercent());
		accountHistory.add(marketName, positionResult);
	}
}

package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Trade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static java.lang.String.format;

public class Account {
	private final Map<MarketName, Position> positions = new HashMap<>();
	private final DrawdownCalc ddCalc;
	private final DownsideDrawdownCalc downsideDrawdownCalc;
	private final AccountHistory accountHistory;
	private double cash;

	public Account(double cash) {
		this.downsideDrawdownCalc = new DownsideDrawdownCalc(cash);
		this.cash = cash;
		this.ddCalc = new DrawdownCalc();
		accountHistory = new AccountHistory(cash);
	}

	public double getMaxDD() {
		return ddCalc.getMaxDD();
	}

	public double getCurrentDD() {
		return ddCalc.getCurrentDD();
	}

	public double getMaxDownsideDD() {
		return downsideDrawdownCalc.getMaxDownsideDrawdown();
	}

	public double getCurrentDownsideDD() {
		return downsideDrawdownCalc.getCurrentDownsideDrawdown();
	}

	public Position getPosition(MarketName market) {
		return positions.get(market);
	}

	public double getPositionValue(MarketName marketName) {
		Position position = positions.get(marketName);
		if (position != null) {
			return position.getQuantity() * position.getLastClose();
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
			position.add(new PositionEntry(entry.time, entry.quantity, entry.price), commission);
		} else {
			positions.put(market, new Position(
					market,
					new PositionEntry(entry.time, entry.quantity, entry.price),
					commission));
		}
		accountHistory.addCommission(commission);
		cash -= cashNeeded;
	}

	public void reducePosition(MarketName market, PositionExit exit, double commission) throws AccountActionException {
		Position position = positions.get(market);
		if (position == null || position.getQuantity() < exit.quantity) {
			throw new AccountActionException(format("Unable to sell %d of %s on %s", exit.quantity, market, exit.time));
		}
		List<PositionPricesRecord> positionPricesRecords = position.reduce(exit, commission);
		accountHistory.addRecords(positionPricesRecords);
		accountHistory.addCommission(commission);
		double value = exit.price * exit.quantity;
		double cashReceived = value - commission;
		cash += cashReceived;
		if (position.getQuantity() == 0) {
			Position removed = positions.remove(market);
			addToHistory(removed);
		}
	}

	public void addCash(double cash) {
		assertPositive(cash);
		downsideDrawdownCalc.changeCash(getClosedEquity(), cash);
		this.cash += cash;
		downsideDrawdownCalc.updateState(getClosedEquity(), getNAV());
		ddCalc.changeCash(cash);
		accountHistory.recordCashPayment(cash);
	}

	public void reduceCash(double cash) throws AccountActionException {
		assertPositive(cash);
		if (cash > this.cash) {
			throw new AccountActionException(format("Unable to withdraw %.2f, only %.2f available", cash, this.cash));
		}
		downsideDrawdownCalc.changeCash(getClosedEquity(), -cash);
		this.cash -= cash;
		downsideDrawdownCalc.updateState(getClosedEquity(), getNAV());
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

	/**
	 * Cash plus entry value of open positions, i.e. NAV without unrealized profits and losses.
	 */
	public double getClosedEquity() {
		double equity = cash;
		for (Position position : positions.values()) {
			equity += position.getOpenEntryValue();
		}
		return equity;
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
		downsideDrawdownCalc.updateState(getClosedEquity(), nav);
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

	/**
	 * Position history with averaged prices: one record per fully closed position, and for each open position
	 * a record of its closed part (if anything was sold) followed by a record of its open part.
	 */
	public List<PositionHistoryRecord> getAllHistoryRecords() {
		List<PositionHistoryRecord> records = new ArrayList<>(accountHistory.getHistoryRecords());
		records.addAll(positions.values().stream().map(Position::getHistoryRecords).flatMap(Collection::stream).toList());
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
		downsideDrawdownCalc.updateState(getClosedEquity(), getNAV());
	}

	/**
	 * Sells whole positions in delisted markets at delisting price, without commission.
	 * Returns executed trades.
	 */
	public List<Trade> processDelistings(List<Delisting> delistings, Time session) throws AccountActionException {
		List<Trade> trades = new ArrayList<>();
		for (Delisting delisting : delistings) {
			Position position = positions.get(delisting.market());
			if (position == null) {
				continue;
			}
			int quantity = position.getQuantity();
			reducePosition(delisting.market(), new PositionExit(session, quantity, delisting.price()), 0.0);
			// Recorded as a regular SELL. Consider a dedicated Delisting action type in the future.
			trades.add(new Trade(delisting.market(), SELL, delisting.price(), quantity, 0.0));
		}
		if (!trades.isEmpty()) {
			double nav = getNAV();
			ddCalc.updateNav(nav);
			downsideDrawdownCalc.updateState(getClosedEquity(), nav);
		}
		return trades;
	}

	public void afterSells() {
		downsideDrawdownCalc.updateState(getClosedEquity(), getNAV());
	}

	private void addToHistory(Position removed) {
		removed.getClosedHistoryRecord().ifPresent(accountHistory::addHistoryRecord);
	}

	/**
	 * Records used for trade statistics: fully closed positions and closed parts of open positions.
	 */
	private List<PositionHistoryRecord> getStatsRecords() {
		List<PositionHistoryRecord> records = new ArrayList<>(accountHistory.getHistoryRecords());
		for (Position position : positions.values()) {
			position.getClosedHistoryRecord().ifPresent(records::add);
		}
		return records;
	}

	public TradeStats getStats() {
		return TradeStatsCalculator.calculate(getStatsRecords());
	}

	public Map<MarketName, TradeStats> getAllStats() {
		return TradeStatsCalculator.calculateByMarket(getStatsRecords());
	}

	public long countProfitableMarkets() {
		return getAllStats().values().stream().filter(s -> s.profitPerTrade() > 0).count();
	}

	public long countUnprofitableMarkets() {
		return getAllStats().values().stream().filter(s -> s.profitPerTrade() < 0).count();
	}

	public String biggestWin() {
		return TradeStatsCalculator.biggestWin(getStatsRecords());
	}

	public String biggestLoss() {
		return TradeStatsCalculator.biggestLoss(getStatsRecords());
	}
}

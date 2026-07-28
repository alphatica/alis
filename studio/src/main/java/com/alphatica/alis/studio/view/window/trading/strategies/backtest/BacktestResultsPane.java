package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountHistory;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.view.tools.SwingHelper.runUiThread;
import static com.alphatica.alis.tools.java.StringHelper.emptyOnNull;
import static com.alphatica.alis.trading.order.Order.*;
import static java.lang.String.format;

final class BacktestResultsPane extends JSplitPane {

	private static final String[] ORDERS_COLUMNS = {"Time", MARKET_ATTRIBUTE_NAME, ORDER_ATTRIBUTE_NAME, "Size", PRIORITY_ATTRIBUTE_NAME};
	private static final String[] TRADES_COLUMNS = {"Market", "Entry time", "Exit time", "Entry price", "Exit price", "Quantity",
			"Profit %", "Profit (cash)", "Entry efficiency", "Exit efficiency"};
	private static final String[] STATS_COLUMNS = {"Metric", "Value"};
	private final ReadOnlyTableModel ordersModel = model(ORDERS_COLUMNS);
	private final ReadOnlyTableModel tradesModel = model(TRADES_COLUMNS);
	private final ReadOnlyTableModel statsModel = model(STATS_COLUMNS);

	BacktestResultsPane() {
		super(JSplitPane.VERTICAL_SPLIT);
		setResizeWeight(0.33);
		setDividerSize(5);
		setTopComponent(tablePane("Orders", ordersModel));
		JSplitPane bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottom.setResizeWeight(0.5);
		bottom.setDividerSize(5);
		bottom.setTopComponent(tablePane("Trades", tradesModel));
		bottom.setBottomComponent(tablePane("Stats", statsModel));
		setBottomComponent(bottom);
	}

	private static ReadOnlyTableModel model(String[] columns) {
		return new ReadOnlyTableModel(new Object[][]{}, columns);
	}

	private static JScrollPane tablePane(String title, ReadOnlyTableModel model) {
		JScrollPane scrollPane = new JScrollPane(new JTable(model));
		scrollPane.setBorder(new TitledBorder(title));
		return scrollPane;
	}

	void clear() {
		ordersModel.setRowCount(0);
		tradesModel.setRowCount(0);
		statsModel.setRowCount(0);
	}

	void update(Time time, List<Order> orders, List<PositionPricesRecord> pricesRecords) {
		List<Order> ordersCopy = new ArrayList<>(orders);
		List<PositionPricesRecord> pricesCopy = new ArrayList<>(pricesRecords);
		runUiThread(() -> addOrders(time, ordersCopy));
		runUiThread(() -> addTrades(pricesCopy));
	}

	private void addOrders(Time time, List<Order> orders) {
		for (Order order : orders) {
			Map<String, String> attributes = order.toAttributes();
			ordersModel.addRow(new Object[]{time.toString(), attributes.get(MARKET_ATTRIBUTE_NAME),
					attributes.get(ORDER_ATTRIBUTE_NAME), attributes.get(SIZE_ATTRIBUTE_NAME), attributes.get(PRIORITY_ATTRIBUTE_NAME)});
		}
		ordersModel.fireTableDataChanged();
	}

	private void addTrades(List<PositionPricesRecord> pricesRecords) {
		for (int i = tradesModel.getRowCount(); i < pricesRecords.size(); i++) {
			PositionPricesRecord record = pricesRecords.get(i);
			tradesModel.addRow(tradeRow(record));
		}
		tradesModel.fireTableDataChanged();
	}

	private static Object[] tradeRow(PositionPricesRecord record) {
		return new Object[]{record.marketName().toString(), record.entryTime().toString(), emptyOnNull(record.exitTime()),
				format("%.2f", record.entry()), format("%.2f", record.exit()), format("%d", record.quantity()),
				format("%.2f%%", record.getProfitPercent()), format("%.2f", record.getProfitCash()),
				format("%.2f", record.getEntryEfficiency()), format("%.2f", record.getExitEfficiency())};
	}

	void fillStats(Account account, StrategyExecutor executor) {
		addStat("Final account value", "%.0f", account.getNAV());
		addStat("Current drawdown", "%.0f %%", account.getCurrentDD());
		addStat("Max drawdown", "%.0f %%", account.getMaxDD());
		addStat("Max downside drawdown", "%.0f %%", account.getMaxDownsideDD());
		AccountHistory history = account.getAccountHistory();
		TradeStats stats = history.getStats();
		addTradeStats(stats, executor);
		addHistoryStats(history);
		statsModel.fireTableDataChanged();
	}

	private void addTradeStats(TradeStats stats, StrategyExecutor executor) {
		addStat("Total trades", "%d", stats.trades());
		addStat("Missed trades", "%d", executor.getMissedTrades());
		addStat("Profit factor", "%.2f", stats.profitFactor());
		addStat("Expectancy", "%.2f", stats.expectancy());
		addStat("Average win", "%.0f %%", stats.averageWinPercent());
		addStat("Average loss", "%.0f %%", stats.averageLossPercent());
		addStat("Average profit per trade", "%.0f %%", stats.profitPerTrade());
		addStat("Average trade length", "%.1f", stats.averageTradeLength());
		addStat("Accuracy", "%.1f %%", stats.accuracy());
	}

	private void addHistoryStats(AccountHistory history) {
		addStat("Paid commissions", "%.0f", history.getPaidCommissions());
		addStat("Profitable markets", "%d", history.countProfitableMarkets());
		addStat("Unprofitable markets", "%d", history.countUnprofitableMarkets());
		addStat("Biggest win", "%s", history.biggestWin());
		addStat("Biggest loss", "%s", history.biggestLoss());
	}

	private void addStat(String name, String pattern, Object value) {
		statsModel.addRow(new Object[]{name, format(pattern, value)});
	}
}

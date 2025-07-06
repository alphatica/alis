package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.tools.AccountActionCSVFacade;
import com.alphatica.alis.studio.tools.GlobalThreadExecutor;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.StrategySelector;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountHistory;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.state.ChangeListeners.addListener;
import static com.alphatica.alis.studio.state.ChangeListeners.publish;
import static com.alphatica.alis.studio.state.StateChange.*;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runUiThread;
import static com.alphatica.alis.tools.java.StringHelper.emptyOnNull;
import static com.alphatica.alis.trading.order.Order.*;
import static java.lang.String.format;

public class BacktestPane extends JPanel {
	private final StrategySelector strategySelector = new StrategySelector();
	private final TimeTextField timeStartField = new TimeTextField("time start", 8);
	private final TimeTextField timeEndField = new TimeTextField("time end", 8);
	private final DoubleTextField commissionRateField = new DoubleTextField("commission rate", 4);
	private final DoubleTextField initialCapitalField = new DoubleTextField("initial capital", 6);
	private final JButton startButton = new JButton("Start");
	private final JButton exportButton = new JButton("Export trades to CSV");
	private final JButton drawDownCheckButton = new JButton("Drawdown check");
	private final JButton compareToRandomButton = new JButton("Compare to random");
	private final List<Double> backtestNavHistory = new ArrayList<>();

	String[] ordersTableColumns = new String[]{"Time", MARKET_ATTRIBUTE_NAME, ORDER_ATTRIBUTE_NAME, "Size", PRIORITY_ATTRIBUTE_NAME};
	private final ReadOnlyTableModel ordersTableModel = new ReadOnlyTableModel(new Object[][]{}, ordersTableColumns);

	String[] tradesTableColumns = new String[]{"Market", "Entry time", "Exit time", "Entry price", "Exit price", "Quantity", "Profit %", "Profit (cash)", "Entry efficiency", "Exit efficiency"};
	private final ReadOnlyTableModel tradesTableModel = new ReadOnlyTableModel(new Object[][]{}, tradesTableColumns);

	String[] statsTableColumns = new String[]{"Metric", "Value"};
	private final ReadOnlyTableModel statsTableModel = new ReadOnlyTableModel(new Object[][]{}, statsTableColumns);

	private Account lastBacktestAccount = null;

	public BacktestPane() {
		setLayout(new BorderLayout());

		// Create main split pane (left: inputs, right: tables)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerSize(5);

		// Left pane: inputs and buttons
		mainSplitPane.setLeftComponent(createLeftPane());

		// Right pane: three vertical split panes for tables
		mainSplitPane.setRightComponent(createRightPane());

		add(mainSplitPane, BorderLayout.CENTER);

		// Add listener for data loaded
		addListener(DATA_LOADED, this::updateDefaults);
	}

	private JPanel createLeftPane() {
		JPanel leftPane = new JPanel(new BorderLayout());
		leftPane.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Input panel with GridBagLayout for two-column layout
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Strategy selector
		gbc.gridx = 0;
		gbc.gridy = 0;
		inputPanel.add(new JLabel("Strategy:"), gbc);
		gbc.gridx = 1;
		inputPanel.add(strategySelector, gbc);

		// Time start
		gbc.gridx = 0;
		gbc.gridy = 1;
		inputPanel.add(new JLabel("Start time:"), gbc);
		gbc.gridx = 1;
		inputPanel.add(timeStartField, gbc);

		// Time end
		gbc.gridx = 0;
		gbc.gridy = 2;
		inputPanel.add(new JLabel("End time:"), gbc);
		gbc.gridx = 1;
		inputPanel.add(timeEndField, gbc);

		// Commission rate
		gbc.gridx = 0;
		gbc.gridy = 3;
		inputPanel.add(new JLabel("Commission rate:"), gbc);
		gbc.gridx = 1;
		inputPanel.add(commissionRateField, gbc);

		// Initial capital
		gbc.gridx = 0;
		gbc.gridy = 4;
		inputPanel.add(new JLabel("Initial capital:"), gbc);
		gbc.gridx = 1;
		inputPanel.add(initialCapitalField, gbc);

		leftPane.add(inputPanel, BorderLayout.NORTH);

		// Button panel with BoxLayout for vertical stacking
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

		// Setup buttons to fill width but maintain normal height
		startButton.addActionListener(a -> startBacktest());
		configureButton(startButton);
		buttonPanel.add(startButton);
		buttonPanel.add(Box.createVerticalStrut(5)); // Space between buttons

		exportButton.addActionListener(a -> exportTradesToCsv());
		exportButton.setEnabled(false);
		configureButton(exportButton);
		buttonPanel.add(exportButton);
		buttonPanel.add(Box.createVerticalStrut(5));

		drawDownCheckButton.addActionListener(a -> checkDrawdown());
		drawDownCheckButton.setEnabled(false);
		configureButton(drawDownCheckButton);
		buttonPanel.add(drawDownCheckButton);
		buttonPanel.add(Box.createVerticalStrut(5));

		compareToRandomButton.addActionListener(a -> compareWithRandom());
		compareToRandomButton.setEnabled(false);
		configureButton(compareToRandomButton);
		buttonPanel.add(compareToRandomButton);

		leftPane.add(buttonPanel, BorderLayout.CENTER);

		return leftPane;
	}

	private void configureButton(JButton button) {
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
	}

	private JSplitPane createRightPane() {
		// Create vertical split panes for tables
		JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		topSplit.setResizeWeight(0.33); // Equal distribution
		topSplit.setDividerSize(5);

		JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottomSplit.setResizeWeight(0.5); // Equal distribution for remaining two
		bottomSplit.setDividerSize(5);

		// Orders table
		JTable ordersTable = new JTable(ordersTableModel);
		JScrollPane ordersScrollPane = new JScrollPane(ordersTable);
		ordersScrollPane.setBorder(new TitledBorder("Orders"));
		topSplit.setTopComponent(ordersScrollPane);

		// Trades table
		JTable tradesTable = new JTable(tradesTableModel);
		JScrollPane tradesScrollPane = new JScrollPane(tradesTable);
		tradesScrollPane.setBorder(new TitledBorder("Trades"));
		bottomSplit.setTopComponent(tradesScrollPane);

		// Stats table
		JTable statsTable = new JTable(statsTableModel);
		JScrollPane statsScrollPane = new JScrollPane(statsTable);
		statsScrollPane.setBorder(new TitledBorder("Stats"));
		bottomSplit.setBottomComponent(statsScrollPane);

		topSplit.setBottomComponent(bottomSplit);

		return topSplit;
	}

	private void setButtonsState(boolean state) {
		startButton.setEnabled(state);
		exportButton.setEnabled(state);
		drawDownCheckButton.setEnabled(state);
		compareToRandomButton.setEnabled(state);
	}

	private void updateDefaults() {
		final int DEFAULT_BARS = 250 * 10;
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			return;
		}
		List<Time> times = marketData.getTimes();
		timeEndField.setText(times.getLast().toString());
		if (times.size() > DEFAULT_BARS) {
			timeStartField.setText(times.get(times.size() - DEFAULT_BARS).toString());
		} else {
			timeStartField.setText(times.getFirst().toString());
		}
		commissionRateField.setText("0.01");
		initialCapitalField.setText("100 000.0");
	}

	private void compareWithRandom() {
		new CompareWithRandomFrame(lastBacktestAccount, timeStartField.getTime(), timeEndField.getTime(), commissionRateField.getDoubleValue(), initialCapitalField.getDoubleValue());
	}

	private void checkDrawdown() {
		new DrawdownCheckFrame(backtestNavHistory);
	}

	private void exportTradesToCsv() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select file to save");

		int userSelection = fileChooser.showSaveDialog(null);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			trySaveActions(lastBacktestAccount.getAccountHistory().getActions(), file);
		}
	}

	private void trySaveActions(List<AccountAction> actions, File file) {
		try {
			AccountActionCSVFacade.saveActions(actions, file);
		} catch (IOException e) {
			ErrorDialog.showError("Unable to save to file", e.getMessage(), e);
		}
	}

	private void startBacktest() {
		ordersTableModel.setRowCount(0);
		tradesTableModel.setRowCount(0);
		statsTableModel.setRowCount(0);
		GlobalThreadExecutor.GLOBAL_EXECUTOR.execute(this::tryExecuteBacktest);
	}

	private void tryExecuteBacktest() {
		try {
			executeBacktest();
		} catch (Exception e) {
			ErrorDialog.showError("Unable to execute strategy", e.getMessage(), e);
		}
	}

	private void executeBacktest() throws AccountActionException {
		Time timeStart = timeStartField.getTime();
		Time timeEnd = timeEndField.getTime();
		double commissionRate = commissionRateField.getDoubleValue();
		double initialCapital = this.initialCapitalField.getDoubleValue();
		Strategy strategy = strategySelector.getValue();
		MarketData marketData = AppState.getMarketData();
		if (!validateBacktestSettings(strategy, marketData, timeStart, timeEnd, initialCapital)) {
			return;
		}
		StrategyExecutor executor = new StrategyExecutor().withTimeRange(timeStart, timeEnd)
														  .withCommissionRate(commissionRate)
														  .withInitialCash(initialCapital)
														  .withBarExecutedConsumer(this::barExecutedCallback);
		publish(BACKTEST_STARTED);
		setButtonsState(false);
		backtestNavHistory.clear();
		Account account = executor.execute(marketData, strategy);
		barExecutedCallback(timeEnd, account, List.of());
		finishBacktest(account, executor);
		publish(BACKTEST_FINISHED);
		setButtonsState(true);
	}

	private void finishBacktest(Account account, StrategyExecutor executor) {
		runUiThread(() -> fillStatsTable(account, executor));
		lastBacktestAccount = account;
	}

	private void fillStatsTable(Account account, StrategyExecutor executor) {
		statsTableModel.addRow(new Object[]{"Final account value", format("%.0f", account.getNAV())});
		statsTableModel.addRow(new Object[]{"Current drawdown", format("%.0f %%", account.getCurrentDD())});
		statsTableModel.addRow(new Object[]{"Max drawdown", format("%.0f %%", account.getMaxDD())});
		AccountHistory history = account.getAccountHistory();
		TradeStats stats = history.getStats();
		statsTableModel.addRow(new Object[]{"Total trades", format("%d", stats.trades())});
		statsTableModel.addRow(new Object[]{"Missed trades", format("%d", executor.getMissedTrades())});
		statsTableModel.addRow(new Object[]{"Profit factor", format("%.2f", stats.profitFactor())});
		statsTableModel.addRow(new Object[]{"Expectancy", format("%.2f", stats.expectancy())});
		statsTableModel.addRow(new Object[]{"Average win", format("%.0f %%", stats.averageWinPercent())});
		statsTableModel.addRow(new Object[]{"Average loss", format("%.0f %%", stats.averageLossPercent())});
		statsTableModel.addRow(new Object[]{"Average profit per trade", format("%.0f %%", stats.profitPerTrade())});
		statsTableModel.addRow(new Object[]{"Accuracy", format("%.1f %%", stats.accuracy())});
		statsTableModel.addRow(new Object[]{"Paid commissions", format("%.0f", history.getPaidCommissions())});
		statsTableModel.addRow(new Object[]{"Profitable markets", format("%d", history.countProfitableMarkets())});
		statsTableModel.addRow(new Object[]{"Unprofitable markets", format("%d", history.countUnprofitableMarkets())});
		statsTableModel.addRow(new Object[]{"Biggest win", format("%s", history.biggestWin())});
		statsTableModel.addRow(new Object[]{"Biggest loss", format("%s", history.biggestLoss())});
		statsTableModel.fireTableDataChanged();
	}

	private void barExecutedCallback(Time time, Account account, List<Order> orders) {
		backtestNavHistory.add(account.getNAV());
		List<Order> copyOrders = new ArrayList<>(orders);
		List<PositionPricesRecord> trades = new ArrayList<>(account.getClosedPricesRecords());
		updateUITables(time, copyOrders, trades);
	}

	private void updateUITables(Time time, List<Order> orders, List<PositionPricesRecord> trades) {
		runUiThread(() -> fillOrdersTable(time, orders));
		runUiThread(() -> fillTradesTable(trades));
	}

	private void fillTradesTable(List<PositionPricesRecord> pricesRecords) {
		for (int i = tradesTableModel.getRowCount(); i < pricesRecords.size(); i++) {
			PositionPricesRecord r = pricesRecords.get(i);
			Object[] row = new Object[]{r.marketName().toString(), r.entryTime().toString(), emptyOnNull(r.exitTime()), format("%.2f", r.entry()),
					format("%.2f", r.exit()), format("%d", r.quantity()), format("%.2f%%", r.getProfitPercent()), format("%.2f", r.getProfitCash()),
					format("%.2f", r.getEntryEfficiency()), format("%.2f", r.getExitEfficiency()),};
			tradesTableModel.addRow(row);
			tradesTableModel.fireTableDataChanged();
		}
	}

	private void fillOrdersTable(Time time, List<Order> orders) {
		for (Order order : orders) {
			Map<String, String> attributes = order.toAttributes();
			Object[] row = new Object[]{time.toString(), attributes.get(MARKET_ATTRIBUTE_NAME), attributes.get(ORDER_ATTRIBUTE_NAME),
					attributes.get(SIZE_ATTRIBUTE_NAME), attributes.get(PRIORITY_ATTRIBUTE_NAME),};
			ordersTableModel.addRow(row);
			ordersTableModel.fireTableDataChanged();
		}
	}

	private boolean validateBacktestSettings(Strategy strategy, MarketData marketData, Time timeStart, Time timeEnd, double initialCapital) {
		final String ERROR = "Unable to start backtest";
		if (strategy == null) {
			return false;
		}
		if (marketData == null) {
			ErrorDialog.showError(ERROR, "Load data first", null);
			return false;
		}
		if (!timeStart.isBefore(timeEnd)) {
			ErrorDialog.showError(ERROR, "Start time is not before end time", null);
			return false;
		}
		if (initialCapital <= 0) {
			ErrorDialog.showError(ERROR, "Initial capital too small", null);
			return false;
		}
		return true;
	}
}
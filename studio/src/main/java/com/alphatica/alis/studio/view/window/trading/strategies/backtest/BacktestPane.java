package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.dao.AccountActionCSVFacade;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.components.ComponentValidationException;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.studio.state.ChangeListeners.publish;
import static com.alphatica.alis.studio.state.StateChange.*;
import static com.alphatica.alis.studio.view.tools.SwingChangeListeners.addUiListener;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runInBackground;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runUiThread;

public class BacktestPane extends JPanel {
	private final BacktestSettingsPanel settingsPanel;
	private final BacktestResultsPane resultsPane = new BacktestResultsPane();
	private final List<Double> backtestNavHistory = new ArrayList<>();

	private Account lastBacktestAccount = null;

	public BacktestPane() {
		setLayout(new BorderLayout());
		settingsPanel = new BacktestSettingsPanel(
				this::startBacktest, this::exportTradesToCsv, this::checkDrawdown, this::compareWithRandom);

		// Create main split pane (left: inputs, right: tables)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerSize(5);

		// Left pane: inputs and buttons
		mainSplitPane.setLeftComponent(settingsPanel);

		// Right pane: three vertical split panes for tables
		mainSplitPane.setRightComponent(resultsPane);

		add(mainSplitPane, BorderLayout.CENTER);

		// Add listener for data loaded
		addUiListener(DATA_LOADED, this::updateDefaults);
	}

	private void setButtonsState(boolean state) {
		settingsPanel.setButtonsEnabled(state);
	}

	private void updateDefaults() {
		final int DEFAULT_BARS = 250 * 10;
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			return;
		}
		List<Time> times = marketData.getTimes();
		settingsPanel.updateDefaults(times, DEFAULT_BARS);
	}

	private void compareWithRandom() {
		BacktestSettings settings = settingsPanel.readSettings();
		new CompareWithRandomFrame(lastBacktestAccount, settings.timeStart(), settings.timeEnd(),
				settings.commissionRate(), settings.initialCapital());
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
		BacktestRequest request;
		try {
			request = readBacktestRequest();
		} catch (ComponentValidationException e) {
			ErrorDialog.showError("Unable to execute strategy", e.getMessage(), null);
			return;
		}
		if (!validateBacktestSettings(request.strategy(), request.marketData(), request.timeStart(), request.timeEnd(), request.initialCapital())) {
			return;
		}

		resultsPane.clear();
		backtestNavHistory.clear();
		setButtonsState(false);
		publish(BACKTEST_STARTED);
		runInBackground(() -> tryExecuteBacktest(request), () -> {
			publish(BACKTEST_FINISHED);
			setButtonsState(true);
		});
	}

	private BacktestRequest readBacktestRequest() {
		BacktestSettings settings = settingsPanel.readSettings();
		return new BacktestRequest(settings.timeStart(), settings.timeEnd(), settings.commissionRate(),
				settings.initialCapital(), settings.strategy(), AppState.getMarketData());
	}

	private void tryExecuteBacktest(BacktestRequest request) {
		try {
			executeBacktest(request);
		} catch (Exception e) {
			ErrorDialog.showError("Unable to execute strategy", e.getMessage(), e);
		}
	}

	private void executeBacktest(BacktestRequest request) throws AccountActionException {
		StrategyExecutor executor = new StrategyExecutor().withTimeRange(request.timeStart(), request.timeEnd())
																  .withCommissionRate(request.commissionRate())
																  .withInitialCash(request.initialCapital())
																  .withBarExecutedConsumer(this::barExecutedCallback);
		Account account = executor.execute(request.marketData(), request.strategy());
		barExecutedCallback(request.timeEnd(), account, List.of());
		finishBacktest(account, executor);
	}

	private void finishBacktest(Account account, StrategyExecutor executor) {
		runUiThread(() -> resultsPane.fillStats(account, executor));
		lastBacktestAccount = account;
	}

	private void barExecutedCallback(Time time, Account account, List<Order> orders) {
		backtestNavHistory.add(account.getNAV());
		resultsPane.update(time, orders, account.getClosedPricesRecords());
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

	private record BacktestRequest(Time timeStart, Time timeEnd, double commissionRate, double initialCapital, Strategy strategy,
			MarketData marketData) {
	}
}

package com.alphatica.alis.studio.view.window.trading.strategies.optimize;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.components.ComponentValidationException;
import com.alphatica.alis.studio.view.window.trading.strategies.optimize.resulttable.ResultTable;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.*;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import com.alphatica.alis.trading.account.scorer.AccountScorer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.state.StateChange.DATA_LOADED;
import static com.alphatica.alis.studio.view.tools.SwingChangeListeners.addUiListener;
public class OptimizationPane extends JPanel {
	private final OptimizationSettingsPanel settingsPanel;
	private final ResultTable resultTable = new ResultTable();
	private final AtomicBoolean stopRequested = new AtomicBoolean(false);
	private final AtomicReference<StrategyOptimizer> strategyOptimizer = new AtomicReference<>();

	public OptimizationPane() {
		setLayout(new BorderLayout());
		settingsPanel = new OptimizationSettingsPanel(this::startOptimization, this::stopOptimization);

		// Create main split pane (left: settings, right: table)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerSize(5);

		// Left pane: settings (inputs and buttons)
		// Set minimum size after layout to ensure content width is respected
		mainSplitPane.setLeftComponent(settingsPanel);

		// Right pane: single result table
		mainSplitPane.setRightComponent(createRightPane());

		add(mainSplitPane, BorderLayout.CENTER);
		addUiListener(DATA_LOADED, this::updateDefaults);

		// Set minimum size of left pane after components are added
		settingsPanel.setMinimumSize(
				new Dimension(settingsPanel.getPreferredSize().width, settingsPanel.getMinimumSize().height));
	}

	private JScrollPane createRightPane() {
		// Single result table
		JScrollPane resultScrollPane = new JScrollPane(resultTable);
		resultScrollPane.setBorder(new TitledBorder("Optimization Results"));
		return resultScrollPane;
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

	private void stopOptimization() {
		stopRequested.set(true);
		StrategyOptimizer optimizer = strategyOptimizer.get();
		if (optimizer != null) {
			optimizer.stop();
		}
		settingsPanel.setStopEnabled(false);
	}

	private void setSettingsInputs(boolean enabled) {
		settingsPanel.setInputsEnabled(enabled);
	}

	private void startOptimization() {
		OptimizationRequest request;
		try {
			request = readOptimizationRequest();
		} catch (ComponentValidationException e) {
			ErrorDialog.showError("Unable to start optimization", e.getMessage(), null);
			return;
		}
		if (request.marketData() == null) {
			ErrorDialog.showError("Unable to start optimization", "Load data first", null);
			return;
		}

		stopRequested.set(false);
		resultTable.clearResults();
		settingsPanel.resetIterations();
		setSettingsInputs(false);
		SwingHelper.runInBackground(() -> tryOptimize(request), this::optimizationFinished);
	}

	private OptimizationRequest readOptimizationRequest() {
		MarketData marketData = AppState.getMarketData();
		Supplier<Strategy> strategyFactory = settingsPanel.getStrategyFactory();
		Time startTime = settingsPanel.getStartTime();
		Time endTime = settingsPanel.getEndTime();
		double initialCapital = settingsPanel.getInitialCapital();
		double commissionRate = settingsPanel.getCommissionRate();
		Supplier<StrategyExecutor> executorFactory = () -> new StrategyExecutor().withInitialCash(initialCapital)
				.withCommissionRate(commissionRate)
				.withTimeRange(startTime, endTime)
				.useCachedMarketData();
		Supplier<AccountScorer> scorerFactory = settingsPanel.getScorerFactory();
		return new OptimizationRequest(strategyFactory, marketData, executorFactory, scorerFactory,
				settingsPanel.getResultVerifier(), settingsPanel.getParametersSelection(), settingsPanel.getMaxPermutations());
	}

	private void tryOptimize(OptimizationRequest request) {
		try {
			StrategyOptimizer optimizer = new StrategyOptimizer(request.strategyFactory(), request.marketData(), request.executorFactory(),
					request.scorerFactory(), request.resultVerifier(), request.parametersSelection(), request.maxCounter());
			strategyOptimizer.set(optimizer);
			optimizer.registerScoreCallback((score, account) -> scoreCallback(optimizer, score, account));
			optimizer.setExceptionCallback(exception -> exceptionCallback(optimizer, exception));
			if (stopRequested.get()) {
				optimizer.stop();
			}
			optimizer.startOptimizations();
		} catch (OptimizerException e) {
			ErrorDialog.showError("Error during optimization", e.getMessage(), e);
		}
	}

	private void optimizationFinished() {
		strategyOptimizer.set(null);
		setSettingsInputs(true);
	}

	private void exceptionCallback(StrategyOptimizer optimizer, Exception ex) {
		optimizer.stop();
		ErrorDialog.showError("Optimization error", ex.toString(), ex);
	}

	private void scoreCallback(StrategyOptimizer optimizer, OptimizerScore newScore, Account account) {
		int count = optimizer.getLoopCount();
		resultTable.scoreCallback(newScore, account);
		SwingHelper.runUiThread(() -> settingsPanel.setIterations(count));
	}

	private record OptimizationRequest(Supplier<Strategy> strategyFactory, MarketData marketData,
			Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier,
			ParametersSelection parametersSelection, long maxCounter) {
	}
}

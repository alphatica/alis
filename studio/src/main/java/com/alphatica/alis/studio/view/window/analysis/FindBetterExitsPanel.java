package com.alphatica.alis.studio.view.window.analysis;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.dao.AccountActionCSVFacade;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.window.analysis.resultable.ResultTable;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.datamining.Runner;
import com.alphatica.alis.trading.datamining.betterexits.DaysInPosition;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitFinder;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;
import com.alphatica.alis.trading.datamining.betterexits.ExitIfSmallProfitAfter;
import com.alphatica.alis.trading.datamining.betterexits.SellStrong;
import com.alphatica.alis.trading.datamining.betterexits.SellWeak;
import com.alphatica.alis.trading.datamining.betterexits.Sma;
import com.alphatica.alis.trading.datamining.betterexits.TrailingStop;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.view.tools.SwingHelper.runInBackground;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runUiThread;

public class FindBetterExitsPanel extends JPanel {
	private final FindBetterExitsSettingsPanel settingsPanel;
	private final ResultTable resultsTable = new ResultTable();
	private final List<AccountAction> accountActions = new ArrayList<>();
	private final AtomicInteger iterationsDone = new AtomicInteger(0);
	private final AtomicBoolean isStarted = new AtomicBoolean(false);
	private final List<Supplier<BetterExitFinder>> exitFinders = List.of(
			Sma::generator,
			DaysInPosition::generator,
			TrailingStop::generator,
			SellWeak::generate,
			SellStrong::generate,
			ExitIfSmallProfitAfter::generate
	);

	private Account account;

	public FindBetterExitsPanel() {
		setLayout(new BorderLayout());
		settingsPanel = new FindBetterExitsSettingsPanel(
				this::loadFile, this::startRunners, this::stopRunners, this::updateScore);

		// Create main split pane (left: settings, right: table)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerSize(5);

		// Left pane: settings (inputs and buttons)
		// Set minimum size after layout to ensure content width is respected
		mainSplitPane.setLeftComponent(settingsPanel);

		// Right pane: single result table
		mainSplitPane.setRightComponent(createRightPane());

		add(mainSplitPane, BorderLayout.CENTER);

		setSettingsInputs(true);

		// Set minimum size of left pane after components are added
		settingsPanel.setMinimumSize(
				new Dimension(settingsPanel.getPreferredSize().width, settingsPanel.getMinimumSize().height));
	}

	private JScrollPane createRightPane() {
		// Single result table
		JScrollPane resultScrollPane = new JScrollPane(resultsTable);
		resultScrollPane.setBorder(new TitledBorder("Exit Analysis Results"));
		return resultScrollPane;
	}

	private void stopRunners() {
		isStarted.set(false);
		settingsPanel.setStopEnabled(false);
	}

	private void startRunners() {
		MarketData marketData = AppState.getMarketData();
		double commissionRate;
		try {
			commissionRate = settingsPanel.getCommissionRate();
			refreshOriginalScore(marketData, commissionRate);
		} catch (Exception exception) {
			ErrorDialog.showError("Invalid settings", exception.getMessage(), exception);
			return;
		}
		isStarted.set(true);
		setSettingsInputs(false);
		resultsTable.clear();
		settingsPanel.setIterations(0);
        iterationsDone.set(0);
		int processors = Runtime.getRuntime().availableProcessors();
		Supplier<AccountScorer> scorerFactory = settingsPanel.getScorerFactory();
		runInBackground(() -> {
			CountDownLatch tasksFinished = startTasks(processors, marketData, scorerFactory, commissionRate);
			waitForFinish(tasksFinished);
		}, () -> setSettingsInputs(true));
	}

	private void waitForFinish(CountDownLatch tasksFinished) {
		try {
			tasksFinished.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			isStarted.set(false);
		}
	}

	private CountDownLatch startTasks(
			int processors,
			MarketData marketData,
			Supplier<AccountScorer> scorerFactory,
			double commissionRate) {
		CountDownLatch tasksFinished = new CountDownLatch(processors);
		for (int i = 0; i < processors; i++) {
			Runnable runnable = buildTask(marketData, scorerFactory, commissionRate);
			runInBackground(() -> {
				try {
					runnable.run();
				} finally {
					tasksFinished.countDown();
				}
			});
		}
		return tasksFinished;
	}

	private Runnable buildTask(
			MarketData marketData,
			Supplier<AccountScorer> scorerFactory,
			double commissionRate) {
		return () -> {
			while (isStarted.get()) {
				Runner runner = new Runner(commissionRate);
				try {
					runner.run(marketData, accountActions, exitFinders, scorerFactory, this::resultCallback);
				} catch (AccountActionException e) {
				}
			}
		};
	}

	private void resultCallback(ExitFinderResult result) {
		int done = iterationsDone.incrementAndGet();
		runUiThread(() -> {
			resultsTable.addResult(result);
			settingsPanel.setIterations(done);
		});
	}

	private void loadFile() {
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			ErrorDialog.showError("Error", "Load data first", null);
		} else {
			loadFileFromFileChooser(marketData);
		}
	}

	private void loadFileFromFileChooser(MarketData marketData) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select a File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			tryReadActions(marketData, fileChooser.getSelectedFile());
		}
	}

	private void tryReadActions(MarketData marketData, File selectedFile) {
		try {
			List<AccountAction> loadedActions = AccountActionCSVFacade.readActions(selectedFile);
			if (loadedActions.isEmpty()) {
				ErrorDialog.showError("File empty", "No account log in the file", null);
			} else {
				applyActions(marketData, selectedFile, loadedActions);
			}
		} catch (Exception e) {
			ErrorDialog.showError("Unable to read actions", e.toString(), e);
		}
	}

	private void applyActions(MarketData marketData, File selectedFile, List<AccountAction> loadedActions) throws AccountActionException {
		accountActions.clear();
		accountActions.addAll(loadedActions);
		settingsPanel.setLoadedFileName(selectedFile.getName());
		refreshOriginalScore(marketData, settingsPanel.getCommissionRate());
		setSettingsInputs(true);
	}

	private void updateScore() {
		if (accountActions.isEmpty()) {
			return;
		}
		try {
			refreshOriginalScore(AppState.getMarketData(), settingsPanel.getCommissionRate());
		} catch (Exception exception) {
			ErrorDialog.showError("Unable to update score", exception.getMessage(), exception);
		}
	}

	private void refreshOriginalScore(MarketData marketData, double commissionRate) throws AccountActionException {
		if (accountActions.isEmpty()) {
			return;
		}
		account = OriginalAccountReplayer.replay(marketData, accountActions, commissionRate);
		AccountScorer scorer = settingsPanel.getScorer();
		settingsPanel.setOriginalScore(scorer.score(account, null));
	}

	private void setSettingsInputs(boolean enabled) {
		settingsPanel.setInputsEnabled(enabled);
	}
}

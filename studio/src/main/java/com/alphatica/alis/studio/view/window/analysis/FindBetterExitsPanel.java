package com.alphatica.alis.studio.view.window.analysis;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.tools.AccountActionCSVFacade;
import com.alphatica.alis.studio.tools.GlobalThreadExecutor;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.studio.view.window.analysis.resultable.ResultTable;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;
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
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;
import static java.lang.String.format;

public class FindBetterExitsPanel extends JPanel {
	private final SmartComboBox<AccountScorer> accountScorerSmartComboBox = new SmartComboBox<>();
	private final JButton loadFileButton = new JButton("Load file");
	private final JLabel loadedFileNameLabel = new JLabel();
	private final JLabel originalScoreLabel = new JLabel();
	private final JLabel iterationsCountLabel = new JLabel();
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JPanel settingsPanel = new JPanel(new GridBagLayout());
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

		// Create main split pane (left: settings, right: table)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setDividerSize(5);

		// Left pane: settings (inputs and buttons)
		JPanel leftPane = createLeftPane();
		// Set minimum size after layout to ensure content width is respected
		mainSplitPane.setLeftComponent(leftPane);

		// Right pane: single result table
		mainSplitPane.setRightComponent(createRightPane());

		add(mainSplitPane, BorderLayout.CENTER);

		// Initialize button states and listeners
		loadFileButton.addActionListener(a -> loadFile());
		startButton.addActionListener(a -> startRunners());
		stopButton.addActionListener(a -> stopRunners());
		accountScorerSmartComboBox.addActionListener(a -> updateScore());
		setSettingsInputs(true);

		// Set minimum size of left pane after components are added
		leftPane.setMinimumSize(new Dimension(leftPane.getPreferredSize().width, leftPane.getMinimumSize().height));
	}

	private JPanel createLeftPane() {
		JPanel leftPane = new JPanel(new BorderLayout());
		leftPane.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Settings panel with GridBagLayout for two-column layout
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Load file
		gbc.gridx = 0;
		gbc.gridy = 0;
		settingsPanel.add(new JLabel("File:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(loadFileButton, gbc);

		// Loaded file name
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Loaded file:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(loadedFileNameLabel, gbc);

		// Result scorer
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Result scorer:"), gbc);
		gbc.gridx = 1;
		accountScorerSmartComboBox.addOption("Net asset value", NetAssetValue::new);
		accountScorerSmartComboBox.addOption("Trade expectancy", Expectancy::new);
		accountScorerSmartComboBox.addOption("NAV / max drawdown", NavAdjustedForMaxDD::new);
		settingsPanel.add(accountScorerSmartComboBox, gbc);

		// Original score
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Original score:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(originalScoreLabel, gbc);

		// Iterations
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Iterations:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(iterationsCountLabel, gbc);

		leftPane.add(settingsPanel, BorderLayout.NORTH);

		// Button panel with BoxLayout for vertical stacking
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

		configureButton(startButton);
		buttonPanel.add(startButton);
		buttonPanel.add(Box.createVerticalStrut(5));

		configureButton(stopButton);
		buttonPanel.add(stopButton);

		leftPane.add(buttonPanel, BorderLayout.CENTER);

		return leftPane;
	}

	private void configureButton(JButton button) {
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		button.setMinimumSize(new Dimension(80, button.getPreferredSize().height));
	}

	private JScrollPane createRightPane() {
		// Single result table
		JScrollPane resultScrollPane = new JScrollPane(resultsTable);
		resultScrollPane.setBorder(new TitledBorder("Exit Analysis Results"));
		return resultScrollPane;
	}

	private void stopRunners() {
		isStarted.set(false);
		setSettingsInputs(true);
	}

	private void startRunners() {
		isStarted.set(true);
		setSettingsInputs(false);
		resultsTable.clear();
		GlobalThreadExecutor.GLOBAL_EXECUTOR.execute(() -> {
			int processors = Runtime.getRuntime().availableProcessors();
			MarketData marketData = AppState.getMarketData();
			List<Thread> threads = startThreads(processors, marketData);
			waitForFinish(threads);
			SwingHelper.runUiThread(() -> setSettingsInputs(true));
		});
	}

	private static void waitForFinish(List<Thread> threads) {
		while (!threads.isEmpty()) {
			try {
				threads.getLast().join();
				threads.removeLast();
			} catch (InterruptedException ex) {
			}
		}
	}

	private List<Thread> startThreads(int processors, MarketData marketData) {
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < processors; i++) {
			Runnable runnable = buildTask(marketData);
			Thread thread = new Thread(runnable);
			threads.add(thread);
			thread.start();
		}
		return threads;
	}

	private Runnable buildTask(MarketData marketData) {
		return () -> {
			while (isStarted.get()) {
				Runner runner = new Runner();
				try {
					runner.run(marketData, accountActions, exitFinders, accountScorerSmartComboBox::getValue, this::resultCallback);
				} catch (AccountActionException e) {
				}
			}
		};
	}

	private void resultCallback(ExitFinderResult result) {
		int done = iterationsDone.incrementAndGet();
		resultsTable.addResult(result);
		SwingHelper.runUiThread(() -> iterationsCountLabel.setText(format("%d", done)));
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
		List<Time> times = getTimes(loadedActions, marketData);
		account = new Account(0.0);
		for (Time time : times) {
			performActionsForTime(time, loadedActions, account);
			if (marketData != null) {
				TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(time, marketData);
				account.updateLastKnown(timeMarketDataSet);
			}
		}
		loadedFileNameLabel.setText(selectedFile.getName());
		updateScore();
		setSettingsInputs(true);
	}

	private void updateScore() {
		AccountScorer scorer = accountScorerSmartComboBox.getValue();
		double score = scorer.score(account, null);
		originalScoreLabel.setText(format("%.1f", score));
	}

	private void setSettingsInputs(boolean enabled) {
		for (Component component : settingsPanel.getComponents()) {
			if (component instanceof JComboBox<?>) {
				component.setEnabled(enabled);
			}
		}
		startButton.setEnabled(enabled);
		stopButton.setEnabled(!enabled);
	}

	private static List<Time> getTimes(List<AccountAction> accountActions, MarketData marketData) {
		if (marketData != null && !accountActions.isEmpty()) {
			return marketData.getTimes().stream().filter(t -> !t.isBefore(accountActions.getFirst().time())).toList();
		} else {
			return accountActions.stream().map(AccountAction::time).toList();
		}
	}
}
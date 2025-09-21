package com.alphatica.alis.studio.view.window.trading.strategies.optimize;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.LongTextField;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.studio.view.tools.components.StrategySelector;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.studio.view.window.trading.strategies.optimize.resulttable.ResultTable;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import com.alphatica.alis.trading.optimizer.StrategyOptimizer;
import com.alphatica.alis.trading.optimizer.OptimizerException;
import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.state.ChangeListeners.addListener;
import static com.alphatica.alis.studio.state.StateChange.DATA_LOADED;
import static com.alphatica.alis.studio.tools.GlobalThreadExecutor.GLOBAL_EXECUTOR;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;

public class OptimizationPane extends JPanel {
	private static final String ITERATIONS_LABEL_PREFIX = "Iterations: ";

	private final StrategySelector strategySelector = new StrategySelector();
	private final TimeTextField timeStartField = new TimeTextField("time start", 8);
	private final TimeTextField timeEndField = new TimeTextField("time end", 8);
	private final DoubleTextField commissionRateField = new DoubleTextField("commission rate", 4);
	private final DoubleTextField initialCapitalField = new DoubleTextField("initial capital", 6);
	private final SmartComboBox<ResultVerifier> resultVerifierComboBox = new SmartComboBox<>();
	private final SmartComboBox<ParametersSelection> parametersSelectionComboBox = new SmartComboBox<>();
	private final SmartComboBox<AccountScorer> backtestScorerComboBox = new SmartComboBox<>();
	private final LongTextField maxPermutationsField = new LongTextField("max permutations", 10);
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private final JLabel iterationCounterLabel = new JLabel(ITERATIONS_LABEL_PREFIX);
	private final JPanel settingsPanel = new JPanel(new GridBagLayout());
	private final ResultTable resultTable = new ResultTable();
	private StrategyOptimizer strategyOptimizer = null;

	public OptimizationPane() {
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
		addListener(DATA_LOADED, this::updateDefaults);

		// Initialize button states and listeners
		startButton.addActionListener(e -> GLOBAL_EXECUTOR.execute(this::optimize));
		stopButton.setEnabled(false);
		stopButton.addActionListener(e -> stopOptimization());
		maxPermutationsField.setText("10000");
		updateMaxPermutationsField();

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

		// Strategy
		gbc.gridx = 0;
		gbc.gridy = 0;
		settingsPanel.add(new JLabel("Strategy:"), gbc);
		gbc.gridx = 1;
		strategySelector.addActionListener(this::strategySelectionChanged);
		settingsPanel.add(strategySelector, gbc);

		// Time start
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Start time:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(timeStartField, gbc);

		// Time end
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("End time:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(timeEndField, gbc);

		// Commission rate
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Commission rate:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(commissionRateField, gbc);

		// Initial capital
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Initial capital:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(initialCapitalField, gbc);

		// Result verifier
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Result verifier:"), gbc);
		gbc.gridx = 1;
		resultVerifierComboBox.addOption("None", () -> ResultVerifier.NONE);
		resultVerifierComboBox.addOption("Remove markets", () -> ResultVerifier.REMOVE_MARKETS);
		resultVerifierComboBox.addOption("Remove orders", () -> ResultVerifier.REMOVE_ORDERS);
		settingsPanel.add(resultVerifierComboBox, gbc);

		// Parameters selection
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Parameters selection:"), gbc);
		gbc.gridx = 1;
		for (ParametersSelection parametersSelection : ParametersSelection.values()) {
			parametersSelectionComboBox.addOption(parametersSelection.getText(), () -> parametersSelection);
		}
		parametersSelectionComboBox.addActionListener(this::parameterSelectionChanged);
		settingsPanel.add(parametersSelectionComboBox, gbc);

		// Result scorer
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Result scorer:"), gbc);
		gbc.gridx = 1;
		backtestScorerComboBox.addOption("Net asset value", NetAssetValue::new);
		backtestScorerComboBox.addOption("Trade expectancy", Expectancy::new);
		backtestScorerComboBox.addOption("NAV / max drawdown", NavAdjustedForMaxDD::new);
		settingsPanel.add(backtestScorerComboBox, gbc);

		// Max permutations
		gbc.gridx = 0;
		gbc.gridy++;
		settingsPanel.add(new JLabel("Max permutations:"), gbc);
		gbc.gridx = 1;
		settingsPanel.add(maxPermutationsField, gbc);

		// Iteration counter
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 2; // Span both columns
		settingsPanel.add(iterationCounterLabel, gbc);

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
		JScrollPane resultScrollPane = new JScrollPane(resultTable);
		resultScrollPane.setBorder(new TitledBorder("Optimization Results"));
		return resultScrollPane;
	}

	private void strategySelectionChanged(ActionEvent actionEvent) {
		updateMaxPermutationsField();
	}

	private void parameterSelectionChanged(ActionEvent e) {
		updateMaxPermutationsField();
	}

	private void updateMaxPermutationsField() {
		long count = getPermutationsCount();
		maxPermutationsField.setValue(count);
		maxPermutationsField.setEnabled(parametersSelectionComboBox.getValue() != FULL_PERMUTATION);
	}

	private long getPermutationsCount() {
		return switch (parametersSelectionComboBox.getValue()) {
			case FULL_PERMUTATION -> StrategyOptimizer.computeAllPermutations(strategySelector.getValue());
			case GENETIC, RANDOM -> 10_000L;
		};
	}

	private void updateDefaults() {
		final int DEFAULT_BARS = 250 * 10;
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			return;
		}
		List<Time> times = marketData.getTimes();
		timeEndField.setText(times.getLast().toString());
		timeStartField.setText(times.size() > DEFAULT_BARS ? times.get(times.size() - DEFAULT_BARS).toString() : times.getFirst().toString());
		commissionRateField.setText("0.01");
		initialCapitalField.setText("100000");
	}

	private void stopOptimization() {
		if (strategyOptimizer != null) {
			strategyOptimizer.stop();
		}
		setSettingsInputs(true);
	}

	private void setSettingsInputs(boolean enabled) {
		for (Component component : settingsPanel.getComponents()) {
			if (component instanceof JTextField || component instanceof JComboBox<?>) {
				component.setEnabled(enabled);
			}
		}
		stopButton.setEnabled(!enabled);
		startButton.setEnabled(enabled);
	}

	private void optimize() {
		MarketData marketData = AppState.getMarketData();
		Supplier<Strategy> strategySupplier = strategySelector::getValue;
		Time startTime = timeStartField.getTime();
		Time endTime = timeEndField.getTime();
		Supplier<StrategyExecutor> strategyExecutorFactory = () -> new StrategyExecutor().withInitialCash(initialCapitalField.getDoubleValue())
																						 .withCommissionRate(commissionRateField.getDoubleValue())
																						 .withTimeRange(startTime, endTime)
																						 .useCachedMarketData();
		resultTable.clearResults();
		iterationCounterLabel.setText(ITERATIONS_LABEL_PREFIX);
		Supplier<AccountScorer> scorerFactory = backtestScorerComboBox::getValue;
		setSettingsInputs(false);
		tryOptimize(strategySupplier, marketData, strategyExecutorFactory, scorerFactory,
				resultVerifierComboBox.getValue(), parametersSelectionComboBox.getValue(), maxPermutationsField.getValue());
	}

	private void tryOptimize(Supplier<Strategy> strategyFactory, MarketData marketData, Supplier<StrategyExecutor> executorFactory,
							 Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) {
		try {
			strategyOptimizer = new StrategyOptimizer(strategyFactory, marketData, executorFactory, scorerFactory, resultVerifier, parametersSelection, maxCounter);
			strategyOptimizer.registerScoreCallback(this::scoreCallback);
			strategyOptimizer.setExceptionCallback(this::exceptionCallback);
			strategyOptimizer.startOptimizations();
			stopOptimization();
		} catch (OptimizerException e) {
			ErrorDialog.showError("Error during optimization", e.getMessage(), e);
		}
	}

	private void exceptionCallback(Exception ex) {
		strategyOptimizer.stop();
		ErrorDialog.showError("Optimization error", ex.toString(), ex);
	}

	private void scoreCallback(OptimizerScore newScore, Account account) {
		GLOBAL_EXECUTOR.execute(() -> {
			int count = strategyOptimizer.getLoopCount();
			SwingHelper.runUiThread(() -> iterationCounterLabel.setText(ITERATIONS_LABEL_PREFIX + " " + count));
			synchronized (OptimizationPane.class) {
				resultTable.scoreCallback(newScore, account);
			}
		});
	}
}
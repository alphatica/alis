package com.alphatica.alis.studio.view.window.trading.strategies.optimize;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.LongTextField;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.studio.view.tools.components.StrategySelector;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.optimizer.Optimizer;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;
import com.alphatica.alis.trading.strategy.Strategy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;

final class OptimizationSettingsPanel extends JPanel {
	private static final String ITERATIONS_LABEL_PREFIX = "Iterations: ";

	private final StrategySelector strategySelector = new StrategySelector();
	private final TimeTextField timeStartField = new TimeTextField("time start", 8);
	private final TimeTextField timeEndField = new TimeTextField("time end", 8);
	private final DoubleTextField commissionRateField = new DoubleTextField("commission rate", 4);
	private final DoubleTextField initialCapitalField = new DoubleTextField("initial capital", 6);
	private final SmartComboBox<ResultVerifier> resultVerifierComboBox = new SmartComboBox<>();
	private final SmartComboBox<ParametersSelection> parametersSelectionComboBox = new SmartComboBox<>();
	private final SmartComboBox<AccountScorer> scorerComboBox = new SmartComboBox<>();
	private final LongTextField maxPermutationsField = new LongTextField("max permutations", 10);
	private final JLabel iterationCounterLabel = new JLabel(ITERATIONS_LABEL_PREFIX);
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");

	OptimizationSettingsPanel(Runnable start, Runnable stop) {
		super(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		configureOptions();
		add(createSettingsPanel(), BorderLayout.NORTH);
		add(createButtonPanel(), BorderLayout.CENTER);
		strategySelector.addActionListener(event -> updateMaxPermutationsField());
		parametersSelectionComboBox.addActionListener(event -> updateMaxPermutationsField());
		startButton.addActionListener(event -> start.run());
		stopButton.addActionListener(event -> stop.run());
		stopButton.setEnabled(false);
		maxPermutationsField.setText("10000");
		updateMaxPermutationsField();
	}

	private void configureOptions() {
		OptimizationOptions.configureResultVerifiers(resultVerifierComboBox);
		OptimizationOptions.configureParametersSelections(parametersSelectionComboBox);
		OptimizationOptions.configureScorers(scorerComboBox);
	}

	private JPanel createSettingsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = constraints();
		addRow(panel, constraints, "Strategy:", strategySelector);
		addRow(panel, constraints, "Start time:", timeStartField);
		addRow(panel, constraints, "End time:", timeEndField);
		addRow(panel, constraints, "Commission rate:", commissionRateField);
		addRow(panel, constraints, "Initial capital:", initialCapitalField);
		addRow(panel, constraints, "Result verifier:", resultVerifierComboBox);
		addRow(panel, constraints, "Parameters selection:", parametersSelectionComboBox);
		addRow(panel, constraints, "Result scorer:", scorerComboBox);
		addRow(panel, constraints, "Max permutations:", maxPermutationsField);
		constraints.gridx = 0;
		constraints.gridwidth = 2;
		panel.add(iterationCounterLabel, constraints);
		return panel;
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(10, 0, 0, 0));
		configureButton(startButton);
		panel.add(startButton);
		panel.add(Box.createVerticalStrut(5));
		configureButton(stopButton);
		panel.add(stopButton);
		return panel;
	}

	private static GridBagConstraints constraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridy = 0;
		return constraints;
	}

	private static void addRow(JPanel panel, GridBagConstraints constraints, String label, JComponent component) {
		constraints.gridx = 0;
		panel.add(new JLabel(label), constraints);
		constraints.gridx = 1;
		panel.add(component, constraints);
		constraints.gridy++;
	}

	private static void configureButton(JButton button) {
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		button.setMinimumSize(new Dimension(80, button.getPreferredSize().height));
	}

	private void updateMaxPermutationsField() {
		long count = switch (parametersSelectionComboBox.getValue()) {
			case FULL_PERMUTATION -> Optimizer.computeAllPermutations(strategySelector.getValue());
			case GENETIC, RANDOM -> 10_000L;
		};
		maxPermutationsField.setValue(count);
		maxPermutationsField.setEnabled(parametersSelectionComboBox.getValue() != FULL_PERMUTATION);
	}

	void updateDefaults(List<Time> times, int defaultBars) {
		timeEndField.setText(times.getLast().toString());
		Time start = times.size() > defaultBars ? times.get(times.size() - defaultBars) : times.getFirst();
		timeStartField.setText(start.toString());
		commissionRateField.setText("0.01");
		initialCapitalField.setText("100000");
	}

	void setInputsEnabled(boolean enabled) {
		strategySelector.setEnabled(enabled);
		timeStartField.setEnabled(enabled);
		timeEndField.setEnabled(enabled);
		commissionRateField.setEnabled(enabled);
		initialCapitalField.setEnabled(enabled);
		resultVerifierComboBox.setEnabled(enabled);
		parametersSelectionComboBox.setEnabled(enabled);
		scorerComboBox.setEnabled(enabled);
		maxPermutationsField.setEnabled(enabled && parametersSelectionComboBox.getValue() != FULL_PERMUTATION);
		startButton.setEnabled(enabled);
		stopButton.setEnabled(!enabled);
	}

	void setStopEnabled(boolean enabled) {
		stopButton.setEnabled(enabled);
	}

	void resetIterations() {
		iterationCounterLabel.setText(ITERATIONS_LABEL_PREFIX);
	}

	void setIterations(int iterations) {
		iterationCounterLabel.setText(ITERATIONS_LABEL_PREFIX + " " + iterations);
	}

	Supplier<Strategy> getStrategyFactory() {
		return strategySelector.getValueSupplier();
	}

	Time getStartTime() {
		return timeStartField.getTime();
	}

	Time getEndTime() {
		return timeEndField.getTime();
	}

	double getCommissionRate() {
		return commissionRateField.getDoubleValue();
	}

	double getInitialCapital() {
		return initialCapitalField.getDoubleValue();
	}

	Supplier<AccountScorer> getScorerFactory() {
		return scorerComboBox.getValueSupplier();
	}

	ResultVerifier getResultVerifier() {
		return resultVerifierComboBox.getValue();
	}

	ParametersSelection getParametersSelection() {
		return parametersSelectionComboBox.getValue();
	}

	long getMaxPermutations() {
		return maxPermutationsField.getValue();
	}
}

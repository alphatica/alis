package com.alphatica.alis.studio.view.window.analysis;

import com.alphatica.alis.studio.view.tools.components.ComponentValidationException;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

final class FindBetterExitsSettingsPanel extends JPanel {

	private final SmartComboBox<AccountScorer> scorerComboBox = new SmartComboBox<>();
	private final DoubleTextField commissionRateField = new DoubleTextField("commission rate", 4);
	private final JButton loadFileButton = new JButton("Load file");
	private final JLabel loadedFileNameLabel = new JLabel();
	private final JLabel originalScoreLabel = new JLabel();
	private final JLabel iterationsCountLabel = new JLabel();
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");

	FindBetterExitsSettingsPanel(Runnable loadFile, Runnable start, Runnable stop, Runnable updateScore) {
		super(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		add(createSettingsPanel(), BorderLayout.NORTH);
		add(createButtonPanel(), BorderLayout.CENTER);
		loadFileButton.addActionListener(event -> loadFile.run());
		startButton.addActionListener(event -> start.run());
		stopButton.addActionListener(event -> stop.run());
		scorerComboBox.addActionListener(event -> updateScore.run());
		commissionRateField.addActionListener(event -> updateScore.run());
		commissionRateField.setText("0.01");
	}

	private JPanel createSettingsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = constraints();
		addRow(panel, constraints, "File:", loadFileButton);
		addRow(panel, constraints, "Loaded file:", loadedFileNameLabel);
		addRow(panel, constraints, "Commission rate:", commissionRateField);
		configureScorers();
		addRow(panel, constraints, "Result scorer:", scorerComboBox);
		addRow(panel, constraints, "Original score:", originalScoreLabel);
		addRow(panel, constraints, "Iterations:", iterationsCountLabel);
		return panel;
	}

	private void configureScorers() {
		scorerComboBox.addOption("Net asset value", NetAssetValue::new);
		scorerComboBox.addOption("Trade expectancy", Expectancy::new);
		scorerComboBox.addOption("NAV / max drawdown", NavAdjustedForMaxDD::new);
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

	private static void configureButton(JButton button) {
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		button.setMinimumSize(new Dimension(80, button.getPreferredSize().height));
	}

	Supplier<AccountScorer> getScorerFactory() {
		return scorerComboBox.getValueSupplier();
	}

	AccountScorer getScorer() {
		return scorerComboBox.getValue();
	}

	double getCommissionRate() {
		double commissionRate = commissionRateField.getDoubleValue();
		if (!Double.isFinite(commissionRate) || commissionRate >= 1) {
			throw new ComponentValidationException("Field `commission rate` must be finite and lower than 1");
		}
		return commissionRate;
	}

	void setLoadedFileName(String fileName) {
		loadedFileNameLabel.setText(fileName);
	}

	void setOriginalScore(double score) {
		originalScoreLabel.setText(String.format("%.1f", score));
	}

	void setIterations(int iterations) {
		iterationsCountLabel.setText(Integer.toString(iterations));
	}

	void setInputsEnabled(boolean enabled) {
		scorerComboBox.setEnabled(enabled);
		commissionRateField.setEnabled(enabled);
		startButton.setEnabled(enabled);
		stopButton.setEnabled(!enabled);
	}

	void setStopEnabled(boolean enabled) {
		stopButton.setEnabled(enabled);
	}
}

package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.StrategySelector;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

final class BacktestSettingsPanel extends JPanel {

	private final StrategySelector strategySelector = new StrategySelector();
	private final TimeTextField timeStartField = new TimeTextField("time start", 8);
	private final TimeTextField timeEndField = new TimeTextField("time end", 8);
	private final DoubleTextField commissionRateField = new DoubleTextField("commission rate", 4);
	private final DoubleTextField initialCapitalField = new DoubleTextField("initial capital", 6);
	private final JButton startButton = new JButton("Start");
	private final JButton exportButton = new JButton("Export trades to CSV");
	private final JButton drawdownCheckButton = new JButton("Drawdown check");
	private final JButton compareToRandomButton = new JButton("Compare to random");

	BacktestSettingsPanel(Runnable start, Runnable export, Runnable checkDrawdown, Runnable compareToRandom) {
		super(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		add(createInputPanel(), BorderLayout.NORTH);
		add(createButtonPanel(), BorderLayout.CENTER);
		startButton.addActionListener(event -> start.run());
		exportButton.addActionListener(event -> export.run());
		drawdownCheckButton.addActionListener(event -> checkDrawdown.run());
		compareToRandomButton.addActionListener(event -> compareToRandom.run());
		exportButton.setEnabled(false);
		drawdownCheckButton.setEnabled(false);
		compareToRandomButton.setEnabled(false);
	}

	private JPanel createInputPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = constraints();
		addRow(panel, constraints, "Strategy:", strategySelector);
		addRow(panel, constraints, "Start time:", timeStartField);
		addRow(panel, constraints, "End time:", timeEndField);
		addRow(panel, constraints, "Commission rate:", commissionRateField);
		addRow(panel, constraints, "Initial capital:", initialCapitalField);
		return panel;
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(10, 0, 0, 0));
		addButton(panel, startButton, true);
		addButton(panel, exportButton, true);
		addButton(panel, drawdownCheckButton, true);
		addButton(panel, compareToRandomButton, false);
		return panel;
	}

	private static void addButton(JPanel panel, JButton button, boolean addSpacing) {
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		panel.add(button);
		if (addSpacing) {
			panel.add(Box.createVerticalStrut(5));
		}
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

	BacktestSettings readSettings() {
		return new BacktestSettings(timeStartField.getTime(), timeEndField.getTime(), commissionRateField.getDoubleValue(),
				initialCapitalField.getDoubleValue(), strategySelector.getValue());
	}

	void updateDefaults(List<Time> times, int defaultBars) {
		timeEndField.setText(times.getLast().toString());
		Time start = times.size() > defaultBars ? times.get(times.size() - defaultBars) : times.getFirst();
		timeStartField.setText(start.toString());
		commissionRateField.setText("0.01");
		initialCapitalField.setText("100 000.0");
	}

	void setButtonsEnabled(boolean enabled) {
		startButton.setEnabled(enabled);
		exportButton.setEnabled(enabled);
		drawdownCheckButton.setEnabled(enabled);
		compareToRandomButton.setEnabled(enabled);
	}
}

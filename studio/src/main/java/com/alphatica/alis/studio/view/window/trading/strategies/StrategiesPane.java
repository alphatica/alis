package com.alphatica.alis.studio.view.window.trading.strategies;

import com.alphatica.alis.studio.view.window.trading.strategies.backtest.BacktestPane;
import com.alphatica.alis.studio.view.window.trading.strategies.optimize.OptimizationPane;
import com.alphatica.alis.studio.view.window.trading.strategies.signals.SignalsPane;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class StrategiesPane extends JPanel {

	public StrategiesPane() {
		GridBagConstraints gbc = setUpPanel();
		addTabs(gbc);
	}

	private GridBagConstraints setUpPanel() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		return gbc;
	}

	private void addTabs(GridBagConstraints gbc) {
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel signals = new SignalsPane();
		tabbedPane.addTab("Signals", signals);
		JPanel backtest = new BacktestPane();
		tabbedPane.addTab("Backtest", backtest);
		JPanel optimization = new OptimizationPane();
		tabbedPane.addTab("Optimization", optimization);
		add(tabbedPane, gbc);
	}

}

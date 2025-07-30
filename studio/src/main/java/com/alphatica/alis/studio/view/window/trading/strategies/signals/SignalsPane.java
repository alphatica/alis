package com.alphatica.alis.studio.view.window.trading.strategies.signals;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.components.StrategySelector;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.tools.java.MarketAttributes;
import com.alphatica.alis.tools.java.StringHelper;
import com.alphatica.alis.trading.strategy.Strategy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.logic.trading.signals.SignalsProvider.getOrdersTableData;
import static com.alphatica.alis.studio.state.ChangeListeners.addListener;
import static com.alphatica.alis.studio.state.StateChange.DATA_LOADED;
import static com.alphatica.alis.studio.view.tools.SwingHelper.buildUiThread;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runOnAction;
import static com.alphatica.alis.tools.java.MarketAttributes.getAttributeNames;

public class SignalsPane extends JPanel {
	private final TimeTextField startTimeTextField = new TimeTextField("time", 8);
	private final StrategySelector strategySelector = new StrategySelector();
	private final JCheckBox applySignalsToPortfolio = new JCheckBox("Show signals for portfolio");
	private final String[] signalsColumnNames = {"Time", "Symbol", "Type", "Priority", "Size"};
	private final JButton showSignalsButton = new JButton("Show signals");
	private final ReadOnlyTableModel signalsTableModel = new ReadOnlyTableModel();
	private final ReadOnlyTableModel customInfoTableModel = new ReadOnlyTableModel();

	public SignalsPane() {
		setLayout(new BorderLayout());

		setupListeners();
		setInitialValues();

		// Add top panel with inputs and labels
		JPanel topPanel = buildTopPanel();
		add(topPanel, BorderLayout.NORTH);

		// Add split pane for tables
		JSplitPane tablesSplitPane = buildTablesSplitPane();
		add(tablesSplitPane, BorderLayout.CENTER);
	}

	private void setInitialValues() {
		applySignalsToPortfolio.setSelected(true);
		startTimeTextField.setEditable(false);
	}

	private void setupListeners() {
		addListener(DATA_LOADED, buildUiThread(this::updateStartTimeTextField));
		applySignalsToPortfolio.addActionListener(e -> applySignalsToPortfolioAction());
	}

	private JPanel buildTopPanel() {
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		JLabel selectStrategyLabel = new JLabel("Strategy:");
		topPanel.add(selectStrategyLabel);
		topPanel.add(strategySelector);

		JLabel startTimeLabel = new JLabel("Start time:");
		topPanel.add(startTimeLabel);
		topPanel.add(startTimeTextField);

		topPanel.add(applySignalsToPortfolio);

		topPanel.add(showSignalsButton);
		runOnAction(showSignalsButton, a -> showSignalsAction());

		return topPanel;
	}

	private JSplitPane buildTablesSplitPane() {
		// Create vertical split pane for tables
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(0.5); // Equal distribution initially
		splitPane.setDividerSize(5);

		// Signals table
		JTable signalsTable = new JTable(signalsTableModel);
		JScrollPane signalsScrollPane = new JScrollPane(signalsTable);
		signalsScrollPane.setBorder(new TitledBorder("Signals")); // Add title for clarity
		splitPane.setTopComponent(signalsScrollPane);

		// Custom info table
		JTable customInfoTable = new JTable(customInfoTableModel);
		JScrollPane customInfoScrollPane = new JScrollPane(customInfoTable);
		customInfoScrollPane.setBorder(new TitledBorder("Strategy Info")); // Add title for clarity
		splitPane.setBottomComponent(customInfoScrollPane);

		return splitPane;
	}

	private void applySignalsToPortfolioAction() {
		updateStartTimeTextField();
		startTimeTextField.setEditable(!applySignalsToPortfolio.isSelected());
	}

	private void showSignalsAction() {
		if (AppState.getMarketData() == null) {
			ErrorDialog.showError("No data loaded", "Load market data first", null);
			return;
		}
		Strategy strategy = strategySelector.getValue();
		if (strategy == null) {
			return;
		}
		try {
			showSignalsButton.setEnabled(false);
			List<String[]> ordersTableData = getOrdersTableData(strategy, applySignalsToPortfolio.isSelected(), startTimeTextField.getTime());
			Collections.reverse(ordersTableData);
			updateSignalsTable(ordersTableData);
			updateCustomInfoTable(strategy.getSummaryTable());
		} catch (Exception ex) {
			ErrorDialog.showError("Unable to get orders table data", ex.toString(), ex);
		} finally {
			showSignalsButton.setEnabled(true);
		}
	}

	private void updateSignalsTable(List<String[]> ordersTableData) {
		Object[][] newData = ordersTableData.toArray(new Object[ordersTableData.size()][]);
		signalsTableModel.setDataVector(newData, signalsColumnNames);
		signalsTableModel.fireTableDataChanged();
	}

	private void updateCustomInfoTable(Map<MarketName, MarketAttributes> customInfo) {
		String[] columnNames = getAttributeNames(customInfo);
		List<String[]> tableData = new ArrayList<>();
		for (Map.Entry<MarketName, MarketAttributes> info : customInfo.entrySet()) {
			String[] tableRow = new String[columnNames.length];
			tableRow[0] = info.getKey().name();
			Map<String, String> dataRow = info.getValue().toAttributes();
			for (int i = 1; i < columnNames.length; i++) {
				tableRow[i] = StringHelper.emptyOnNull(dataRow.get(columnNames[i]));
			}
			tableData.add(tableRow);
		}
		customInfoTableModel.setDataVector(tableData.toArray(new Object[0][]), columnNames);
		customInfoTableModel.fireTableDataChanged();
	}

	private void updateStartTimeTextField() {
		if (!applySignalsToPortfolio.isSelected()) {
			setStartTimeTextFieldToLastTimeFromData();
		} else {
			startTimeTextField.setText("");
		}
	}

	private void setStartTimeTextFieldToLastTimeFromData() {
		MarketData marketData = AppState.getMarketData();
		if (marketData != null) {
			Time last = marketData.getTimes().getLast();
			if (last != null) {
				startTimeTextField.setText(last.toString());
			}
		}
	}
}
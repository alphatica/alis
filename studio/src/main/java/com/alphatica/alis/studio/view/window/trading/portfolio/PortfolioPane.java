package com.alphatica.alis.studio.view.window.trading.portfolio;

import com.alphatica.alis.studio.dao.DaoException;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.tools.AccountActionCSVFacade;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountHistory;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.logic.trading.portfolio.PortfolioProvider.getPricesRecords;
import static com.alphatica.alis.studio.state.ChangeListeners.addListener;
import static com.alphatica.alis.studio.state.StateChange.DATA_LOADED;
import static com.alphatica.alis.studio.state.StateChange.PORTFOLIO_CHANGED;
import static com.alphatica.alis.tools.java.StringHelper.emptyOnNull;
import static java.lang.String.format;

public class PortfolioPane extends JPanel {
	private static final String OVERALL_PROFIT_LABEL = "Overall Profit: ";
	private static final String ACCURACY_LABEL = "Accuracy: ";
	private static final String AV_WIN_LABEL = "Average Win: ";
	private static final String AV_LOSS_LABEL = "Average Loss: ";
	private static final String PROFIT_FACTOR_LABEL = "Profit Factor: ";
	private static final String TRADES_LABEL = "Trades: ";
	private static final String NAV_LABEL = "Net Asset Value: ";
	private static final String CURRENT_DD_LABEL = "Current Drawdown: ";
	private static final String MAX_DD_LABEL = "Maximum Drawdown: ";
	private static final String MAX_DOWNSIDE_DD_LABEL = "Maximum Downside Drawdown: ";
	private static final String CASH_LABEL = "Cash: ";
	private static final String TOTAL_COMMISSIONS_PAID = "Total commissions: ";
	private final String[] historyColumns = {"Market", "Buy time", "Buy price", "Sell time", "Sell price", "Quantity", "Value", "Profit (cash)",
			"Profit (%)", "Buy efficiency", "Sell efficiency"};
	private final String[] actionsColumns = {"Time", "Market", "Type", "Price", "Quantity", "Value", "Commission"};
	private final JLabel overallCashProfitLabel = new JLabel(OVERALL_PROFIT_LABEL);
	private final JLabel accuracyLabel = new JLabel(ACCURACY_LABEL);
	private final JLabel avWinLabel = new JLabel(AV_WIN_LABEL);
	private final JLabel avLossLabel = new JLabel(AV_LOSS_LABEL);
	private final JLabel profitFactorLabel = new JLabel(PROFIT_FACTOR_LABEL);
	private final JLabel tradesLabel = new JLabel(TRADES_LABEL);
	private final JLabel navLabel = new JLabel(NAV_LABEL);
	private final JLabel currentDDLabel = new JLabel(CURRENT_DD_LABEL);
	private final JLabel maxDDLabel = new JLabel(MAX_DD_LABEL);
	private final JLabel maxDownsizeDDLabel = new JLabel(MAX_DOWNSIDE_DD_LABEL);
	private final JLabel cashLabel = new JLabel(CASH_LABEL);
	private final JLabel totalCommissionsPaidLabel = new JLabel(TOTAL_COMMISSIONS_PAID);
	private transient Account account;

	public PortfolioPane() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = prepareConstraints(GridBagConstraints.BOTH);

		// Add tables in a vertical split pane
		JSplitPane tablesSplitPane = createTablesSplitPane();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		add(tablesSplitPane, gbc);

		// Add stats labels
		createAndAddStatsLabels(gbc);

		// Add action buttons
		createAndAddActionsButtons(gbc);

		// Set minimum size to prevent pane from being narrower than content
		setMinimumSize(new Dimension(getPreferredSize().width, getMinimumSize().height));
	}

	private JSplitPane createTablesSplitPane() {
		// Create vertical split pane for tables
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setResizeWeight(0.5);
		splitPane.setDividerSize(5);

		// Actions table
		JScrollPane actionsScrollPane = createActionsTable();
		actionsScrollPane.setBorder(new TitledBorder("Actions"));
		splitPane.setTopComponent(actionsScrollPane);

		// History table
		JScrollPane historyScrollPane = createHistoryTable();
		historyScrollPane.setBorder(new TitledBorder("Trade History"));
		splitPane.setBottomComponent(historyScrollPane);

		return splitPane;
	}

	private void createAndAddActionsButtons(GridBagConstraints gbc) {
		JPanel inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints inputGbc = prepareConstraints(GridBagConstraints.HORIZONTAL);
		inputGbc.gridy = 0;

		setupAddTradeButton(inputGbc, inputPanel);
		setupImportTradesButton(inputGbc, inputPanel);
		setupDepositWithdrawalButton(inputGbc, inputPanel);

		gbc.gridy = 2;
		gbc.weighty = 0.0;
		gbc.gridwidth = 2;
		add(inputPanel, gbc);
	}

	private void setupDepositWithdrawalButton(GridBagConstraints inputGbc, JPanel inputPanel) {
		inputGbc.gridx = 2;
		JButton addRemoveCashButton = new JButton("Add / Remove cash...");
		configureButton(addRemoveCashButton); // Apply BacktestPane button styling
		addRemoveCashButton.addActionListener(a -> new AddRemoveCashFrame());
		inputPanel.add(addRemoveCashButton, inputGbc);
	}

	private void setupImportTradesButton(GridBagConstraints inputGbc, JPanel inputPanel) {
		inputGbc.gridx = 1;
		JButton importTradesButton = new JButton("Import trades from CSV...");
		configureButton(importTradesButton); // Apply BacktestPane button styling
		importTradesButton.addActionListener(a -> chooseImportTradesFile());
		inputPanel.add(importTradesButton, inputGbc);
	}

	private void chooseImportTradesFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select a File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			try {
				File selectedFile = fileChooser.getSelectedFile();
				List<AccountAction> accountActions = AccountActionCSVFacade.readActions(selectedFile);
				AccountProvider.saveActions(accountActions);
			} catch (Exception e) {
				ErrorDialog.showError("Unable to read actions", e.toString(), e);
			}
		}
	}

	private void setupAddTradeButton(GridBagConstraints inputGbc, JPanel inputPanel) {
		inputGbc.gridx = 0;
		JButton addTradeButton = new JButton("Add trade...");
		configureButton(addTradeButton); // Apply BacktestPane button styling
		addTradeButton.addActionListener(a -> new AddTradeFrame());
		inputPanel.add(addTradeButton, inputGbc);
	}

	private void configureButton(JButton button) {
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		button.setMinimumSize(new Dimension(80, button.getPreferredSize().height));
	}

	private void createAndAddStatsLabels(GridBagConstraints gbc) {
		JPanel labelPanel = createStatsLabels();
		gbc.gridy = 1;
		gbc.weighty = 0.0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(labelPanel, gbc);
	}

	private JPanel createStatsLabels() {
		JPanel labelPanel = new JPanel(new GridLayout(3, 4, 5, 5));
		labelPanel.add(navLabel);
		labelPanel.add(tradesLabel);
		labelPanel.add(cashLabel);
		labelPanel.add(accuracyLabel);
		labelPanel.add(avWinLabel);
		labelPanel.add(avLossLabel);
		labelPanel.add(currentDDLabel);
		labelPanel.add(maxDDLabel);
		labelPanel.add(overallCashProfitLabel);
		labelPanel.add(totalCommissionsPaidLabel);
		labelPanel.add(profitFactorLabel);
		labelPanel.add(maxDownsizeDDLabel);
		return labelPanel;
	}

	private JScrollPane createHistoryTable() {
		ReadOnlyTableModel tableModel = new ReadOnlyTableModel(new Object[][]{}, historyColumns);
		JTable historyTable = new JTable(tableModel);
		tryUpdateHistoryTable(tableModel);
		addListener(PORTFOLIO_CHANGED, () -> tryUpdateHistoryTable(tableModel));
		addListener(DATA_LOADED, () -> tryUpdateHistoryTable(tableModel));
		return new JScrollPane(historyTable);
	}

	private void tryUpdateHistoryTable(ReadOnlyTableModel historyTableModel) {
		try {
			updateHistoryTable(historyTableModel);
		} catch (Exception ex) {
			ErrorDialog.showError("Unable to read account actions", ex.toString(), ex);
		}
	}

	private void updateHistoryTable(ReadOnlyTableModel historyTableModel) throws DaoException, AccountActionException {
		account = new Account(0);
		List<AccountAction> accountActions = AccountProvider.getAccountActions();
		List<PositionPricesRecord> pricesRecords = getPricesRecords(accountActions, account);
		Collections.reverse(pricesRecords);
		boolean haveMarketData = AppState.getMarketData() != null;
		updateStatsLabels(haveMarketData);
		Object[][] data = new Object[pricesRecords.size()][];
		int index = 0;
		for (PositionPricesRecord r : pricesRecords) {
			data[index++] = new Object[]{r.marketName(), r.entryTime().toString(), format("%.2f", r.entry()), emptyOnNull(r.exitTime()),
					showData(r.exitTime() != null || haveMarketData, r.exit()), format("%d", r.quantity()), showData(haveMarketData, r.value()),
					showData(r.exitTime() != null || haveMarketData, r.getProfitCash()), showData(r.exitTime() != null || haveMarketData,
					r.getProfitPercent()), showData(haveMarketData, r.getEntryEfficiency()), showData(haveMarketData, r.getExitEfficiency()),};
		}
		if (data.length != 0) {
			historyTableModel.setDataVector(data, historyColumns);
			historyTableModel.fireTableDataChanged();
		}
	}

	private void updateStatsLabels(boolean haveMarketData) {
		String oneDecimalPercentDigitFormat = "%.1f %%";
		AccountHistory accountHistory = account.getAccountHistory();
		TradeStats stats = accountHistory.getStats();

		String trades = format("%d", stats.trades());
		tradesLabel.setText(TRADES_LABEL + trades);

		String cash = format("%.0f", account.getCash());
		cashLabel.setText(CASH_LABEL + cash);

		String commissions = format("%.0f", accountHistory.getPaidCommissions());
		totalCommissionsPaidLabel.setText(TOTAL_COMMISSIONS_PAID + commissions);

		String accuracy = format(oneDecimalPercentDigitFormat, stats.accuracy());
		accuracyLabel.setText(ACCURACY_LABEL + accuracy);

		String avWin = format(oneDecimalPercentDigitFormat, stats.averageWinPercent());
		avWinLabel.setText(AV_WIN_LABEL + avWin);

		String avLoss = format(oneDecimalPercentDigitFormat, stats.averageLossPercent());
		avLossLabel.setText(AV_LOSS_LABEL + avLoss);

		String profitFactor = format("%.2f", stats.profitFactor());
		profitFactorLabel.setText(PROFIT_FACTOR_LABEL + profitFactor);

		if (haveMarketData) {
			String cashProfit = format("%.0f", account.calcCashProfit());
			overallCashProfitLabel.setText(OVERALL_PROFIT_LABEL + cashProfit);

			String nav = format("%.0f", account.getNAV());
			navLabel.setText(NAV_LABEL + nav);

			String currentDD = format("%.0f %%", account.getCurrentDD());
			currentDDLabel.setText(CURRENT_DD_LABEL + currentDD);

			String maxDD = format("%.0f %%", account.getMaxDD());
			maxDDLabel.setText(MAX_DD_LABEL + maxDD);

			String maxDownsideDD = format(oneDecimalPercentDigitFormat, account.getMaxDownsideDD());
			maxDownsizeDDLabel.setText(MAX_DOWNSIDE_DD_LABEL + maxDownsideDD);
		}
	}

	private String showData(boolean show, double value) {
		if (show) {
			return format("%.2f", value);
		} else {
			return "";
		}
	}

	private JScrollPane createActionsTable() {
		ReadOnlyTableModel tableModel = new ReadOnlyTableModel(new Object[][]{}, actionsColumns);
		JTable actionsTable = new JTable(tableModel);
		addListener(PORTFOLIO_CHANGED, () -> tryUpdateActionsTable(tableModel));
		tryUpdateActionsTable(tableModel);
		return new JScrollPane(actionsTable);
	}

	private GridBagConstraints prepareConstraints(int fill) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = fill;
		return gbc;
	}

	private void tryUpdateActionsTable(ReadOnlyTableModel tableModel) {
		try {
			updateActionsTable(tableModel);
		} catch (DaoException ex) {
			ErrorDialog.showError("Unable to read account actions", ex.toString(), ex);
		}
	}

	private void updateActionsTable(ReadOnlyTableModel tableModel) throws DaoException {
		List<AccountAction> accountActions = AccountProvider.getAccountActions();
		Collections.reverse(accountActions);
		Object[][] data = new Object[accountActions.size()][];
		int index = 0;
		for (AccountAction action : accountActions) {
			data[index++] = buildRow(action);
		}
		tableModel.setDataVector(data, actionsColumns);
		tableModel.fireTableDataChanged();
	}

	private Object[] buildRow(AccountAction action) {
		Object[] row = new Object[actionsColumns.length];
		int index = 0;
		Map<String, String> actionRow = action.toAttributes();
		for (String key : actionsColumns) {
			row[index++] = emptyOnNull(actionRow.get(key));
		}
		return row;
	}
}
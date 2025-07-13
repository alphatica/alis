package com.alphatica.alis.studio.view.window.trading.strategies.optimize.resulttable;

import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.optimizer.OptimizerScore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static java.lang.String.format;

public class ResultTable extends JTable {

	private static final String[] RESULTS_COLUMNS = {"Score", "NAV", "Max DD", "Trades", "Accuracy", "Expectancy", "Profit factor", "Profit per trade", "Params"};
	private static final int PARAMS_COLUMN_INDEX = 8;

	private final ReadOnlyTableModel resultsTableModel = new ReadOnlyTableModel(new Object[][]{}, RESULTS_COLUMNS);
	private final List<ResultsTableRow> resultsTableRows = new ArrayList<>();

	public ResultTable() {
		setModel(resultsTableModel);
		getColumnModel().getColumn(PARAMS_COLUMN_INDEX).setCellRenderer(new LinkCellRenderer());
		setupMouseListener();
	}

	public void scoreCallback(OptimizerScore newScore, Account account) {
		addToTableRows(newScore, account);
		rebuildUITable();
	}

	public void clearResults() {
		resultsTableModel.setDataVector(new Object[][]{}, RESULTS_COLUMNS);
		resultsTableRows.clear();
	}

	private void setupMouseListener() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				if (col == PARAMS_COLUMN_INDEX) {
					showDetailsFrame(resultsTableRows.get(row));
				}
			}
		});
	}

	private void rebuildUITable() {
		Object[][] tableRows = resultsTableRows.stream().map(this::mapRowToValues).toArray(Object[][]::new);
		SwingHelper.runUiThread(() -> {
			resultsTableModel.setDataVector(tableRows, RESULTS_COLUMNS);
			resultsTableModel.fireTableDataChanged();
		});
	}

	private Object[] mapRowToValues(ResultsTableRow tr) {
		return new Object[]{
				format("%.0f", tr.score().score()),
				format("%.0f", tr.NAV()),
				format("%.0f %%", tr.maxDD()),
				format("%d", tr.trades()),
				format("%.1f %%", tr.accuracy()),
				format("%.2f", tr.expectancy()),
				format("%.2f", tr.profitFactor()),
				format("%.2f", tr.profitPerTrade()),
				"View"
		};
	}

	private void addToTableRows(OptimizerScore score, Account account) {
		ResultsTableRow row = createResultsRow(score, account);
		resultsTableRows.add(row);
		resultsTableRows.sort(Comparator.comparingDouble(r -> -r.score().score()));
		if (resultsTableRows.size() > 100) {
			resultsTableRows.removeLast();
		}
	}

	private ResultsTableRow createResultsRow(OptimizerScore score, Account account) {
		TradeStats stats = account.getAccountHistory().getStats();
		return new ResultsTableRow(
				account.getNAV(), account.getMaxDD(), stats.trades(), stats.accuracy(),
				stats.expectancy(), stats.profitFactor(), stats.profitPerTrade(), score
		);
	}

	private void showDetailsFrame(ResultsTableRow tr) {
		List<String> content = tr.score().formatParamsAsJavaCode();
		String text = buildText(content);

		// Create JTextArea with dynamic rows and columns
		JTextArea detailsArea = new JTextArea(text);
		detailsArea.setEditable(false);
		detailsArea.setRows(Math.min(content.size() + 2, 20)); // Up to 20 rows, +2 for padding
		detailsArea.setColumns(longestLineLen(content) + 5); // Add padding to longest line
		detailsArea.setLineWrap(false); // Preserve code formatting
//		detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, detailsArea.getFont().getSize())); // Use monospaced font for code

		// Add padding via border
		detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Wrap in JScrollPane
		JScrollPane scrollPane = new JScrollPane(detailsArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove default border for cleaner look

		// Create frame
		JFrame detailsFrame = new JFrame(format("%.0f", tr.score().score()));
		detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		detailsFrame.add(scrollPane, BorderLayout.CENTER);

		// Set minimum and maximum sizes
		detailsFrame.setMinimumSize(new Dimension(200, 200));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		detailsFrame.setMaximumSize(new Dimension((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8)));

		// Use pack() to size frame naturally
		detailsFrame.pack();

		// Center on screen
		detailsFrame.setLocationRelativeTo(null);
		detailsFrame.setVisible(true);
	}

	private String buildText(List<String> content) {
		StringBuilder sb = new StringBuilder();
		for (String line : content) {
			sb.append(line);
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	private int longestLineLen(List<String> content) {
		return content.stream().mapToInt(String::length).max().orElse(10); // Default to 10 if empty
	}
}
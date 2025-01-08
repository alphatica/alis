package com.alphatica.alis.studio.view.window.analysis.resultable;

import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.models.ReadOnlyTableModel;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;

import javax.swing.JTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class ResultTable extends JTable {
	private static final String[] RESULTS_COLUMNS = {"Score", "Early exits", "Name", "Description"};

	private final ReadOnlyTableModel resultsTableModel = new ReadOnlyTableModel(new Object[][]{}, RESULTS_COLUMNS);
	private final List<ExitFinderResult> results = new ArrayList<>();
	public ResultTable() {
		setModel(resultsTableModel);
		getTableHeader().setResizingAllowed(true);
	}

	public synchronized void addResult(ExitFinderResult newResult) {
		if (alreadyHas(newResult)) {
			return;
		}
		results.add(newResult);
		Collections.sort(results);
		Collections.reverse(results);
		if (results.size() > 100) {
			ExitFinderResult removed = results.removeLast();
			if (!removed.equals(newResult)) {
				rebuildUITable();
			}
		} else {
			rebuildUITable();
		}
	}

	public void clear() {
		results.clear();
		rebuildUITable();
	}

	private boolean alreadyHas(ExitFinderResult result) {
		return results.stream().anyMatch(checking -> checking.name().equals(result.name()));
	}

	private void rebuildUITable() {
		Object[][] tableRows = results.stream().map(this::mapResultToRow).toArray(Object[][]::new);
		SwingHelper.runUiThread(() -> {
			resultsTableModel.setDataVector(tableRows, RESULTS_COLUMNS);
			resultsTableModel.fireTableDataChanged();
		});
	}

	private Object[] mapResultToRow(ExitFinderResult result) {
		return new Object[]{
				format("%.1f", result.score()),
				format("%d", result.trades()),
				result.name(),
				result.description()
		};
	}
}
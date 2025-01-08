package com.alphatica.alis.studio.view.window.data;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.studio.logic.data.stooq.StooqDataProvider;
import com.alphatica.alis.studio.state.AppState;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.studio.state.ChangeListeners.addListener;
import static com.alphatica.alis.studio.state.ChangeListeners.bindLabelToEvent;
import static com.alphatica.alis.studio.state.StateChange.DATA_LOADED;
import static com.alphatica.alis.studio.state.StateChange.DATA_STATUS_CHANGED;
import static com.alphatica.alis.studio.view.tools.SwingHelper.buildUiThread;
import static com.alphatica.alis.studio.view.tools.SwingHelper.createHtmlLinkLabel;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runOnAction;

public class StooqPane {

	private static final String[] COLUMN_NAMES = {"Name", "Type", "First", "Last", "Price"};

	private StooqPane() {
	}

	public static JPanel getPanel() {
		JPanel mainPanel = createMainPanel();
		JPanel topPanel = createTopRowPanel();
		JScrollPane scrollPane = createDataTable();
		finishPanel(mainPanel, topPanel, scrollPane);
		return mainPanel;
	}

	private static void finishPanel(JPanel mainPanel, JPanel topPanel, JScrollPane scrollPane) {
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
	}

	private static JScrollPane createDataTable() {

		DefaultTableModel tableModel = new DefaultTableModel(new Object[][]{}, COLUMN_NAMES);
		addUpdateListener(tableModel);
		JTable table = new JTable(tableModel);
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);

		return new JScrollPane(table);
	}

	private static void addUpdateListener(DefaultTableModel tableModel) {
		addListener(DATA_LOADED, buildUiThread(() -> updateTable(tableModel)));
	}

	private static void updateTable(DefaultTableModel tableModel) {
		List<String[]> data = new ArrayList<>();
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			return;
		}
		for (Market market : marketData.listMarkets(ALL)) {
			TimeMarketData first = market.getAtOrNext(new Time(0));
			if (first == null) {
				continue;
			}
			TimeMarketData last = market.getAtOrPrevious(new Time(Long.MAX_VALUE));
			String[] row = {market.getName().toString(), market.getType().toString(), first.getTime().toString(), last.getTime().toString(),
					String.format("%.2f", last.getData(Layer.CLOSE, 0))};
			data.add(row);
		}
		data.sort(Comparator.comparing(o -> o[0]));
		Object[][] array = data.toArray(new Object[0][]);
		tableModel.setRowCount(data.size());
		tableModel.setDataVector(array, COLUMN_NAMES);
	}

	private static JPanel createMainPanel() {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		return mainPanel;
	}

	private static JPanel createTopRowPanel() {
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

		JButton unzip = createUnzipButton();
		JButton loadPl = createLoadPLButton();
		JButton loadUs = createLoadUSButton();
		JLabel dataStatus = new JLabel();
		bindLabelToEvent(dataStatus, DATA_STATUS_CHANGED, AppState::getDataStatus);
		JLabel webLink = createHtmlLinkLabel("Open Stooq.pl", "https://stooq.pl/db/h/");

		topPanel.add(webLink);
		topPanel.add(unzip);
		topPanel.add(loadPl);
		topPanel.add(loadUs);
		topPanel.add(dataStatus);
		return topPanel;
	}

	private static JButton createLoadPLButton() {
		JButton load = new JButton("Load PL");
		runOnAction(load, e -> StooqDataProvider.loadPLData());
		return load;
	}

	private static JButton createLoadUSButton() {
		JButton load = new JButton("Load US");
		runOnAction(load, e -> StooqDataProvider.loadUSData());
		return load;
	}

	private static JButton createUnzipButton() {
		JButton unzip = new JButton("Unzip");
		runOnAction(unzip, a -> StooqDataProvider.unzipNewData());
		return unzip;
	}

}

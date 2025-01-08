package com.alphatica.alis.studio.logic.data.stooq;

import com.alphatica.alis.data.loader.ThrowingMarketDataSupplier;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.view.tools.ErrorDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

public class StooqDataProvider {
	private static final String GPW_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_gpw";
	private static final String US_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_us";

	public static void loadUSData() {
		handleDataLoading(() -> StooqLoader.loadUS(US_TARGET_DATA_DIR));
	}

	@SuppressWarnings("java:S2142")
	public static void loadPLData() {
		handleDataLoading(() -> StooqLoader.loadPL(GPW_TARGET_DATA_DIR));
	}

	private static void handleDataLoading(ThrowingMarketDataSupplier loader) {
		try {
			AppState.setDataStatus("Loading data...");
			MarketData marketData = loader.get();
			if (marketData.listMarkets(ALL).isEmpty()) {
				AppState.setDataStatus("No data found");
				ErrorDialog.showError("No data", "Empty data", null);
			} else {
				AppState.setMarketData(marketData);
				AppState.setDataStatus("Data loaded");
			}
		} catch (Exception ex) {
			ErrorDialog.showError("Unable to load new data", ex.toString(), ex);
		}
	}

	public static void unzipNewData() {
		List<String> directories = List.of("Pobrane", "Downloads");
		AppState.setDataStatus("Unzipping new data...");
		for (String directory : directories) {
			if (tryDir(directory)) {
				AppState.setDataStatus("Data unzipped");
				return;
			}
		}
		AppState.setDataStatus("Data not found");
		ErrorDialog.showError("Data not found", "Download data from Stooq.pl first", null);
	}

	private static boolean tryDir(String dir) {
		try {
			var okPl = StooqLoader.unzipNewPL(GPW_TARGET_DATA_DIR, dir);
			var okUS = StooqLoader.unzipNewUS(US_TARGET_DATA_DIR, dir);
			return okPl || okUS;
		} catch (IOException e) {
			AppState.setDataStatus("Unzip failed");
			ErrorDialog.showError("Unable to unzip", e.toString(), e);
		}
		return false;
	}

	private StooqDataProvider() {
	}
}

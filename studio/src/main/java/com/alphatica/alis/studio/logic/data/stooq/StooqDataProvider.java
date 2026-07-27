package com.alphatica.alis.studio.logic.data.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.loader.ThrowingMarketDataSupplier;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.state.AppState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.DATA_NOT_FOUND;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.LOAD_FAILED;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.NO_DATA;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.UNZIP_FAILED;
import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

public class StooqDataProvider {
	private static final String GPW_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_gpw";
	private static final String US_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_us";

	public static void loadUSData() {
		handleDataLoading(() -> StooqLoader.loadUS(US_TARGET_DATA_DIR));
	}

	@SuppressWarnings("java:S2142")
	public static void loadPLData() {
		loadPLData(Path.of(GPW_TARGET_DATA_DIR));
	}

	@SuppressWarnings("java:S2142")
	public static void loadPLData(Path dataDirectory) {
		handleDataLoading(() -> StooqLoader.loadPL(dataDirectory.toString()));
	}

	private static void handleDataLoading(ThrowingMarketDataSupplier loader) {
		AppState.setDataStatus("Loading data...");
		MarketData marketData;
		try {
			marketData = loader.get();
		} catch (Exception ex) {
			AppState.setDataStatus("Unable to load data");
			throw new DataProcessingException(LOAD_FAILED, ex);
		}
		if (marketData.listMarkets(ALL).isEmpty()) {
			AppState.setDataStatus("No data found");
			throw new DataProcessingException(NO_DATA);
		}
		AppState.setMarketData(marketData);
		AppState.setDataStatus("Data loaded");
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
		throw new DataProcessingException(DATA_NOT_FOUND);
	}

	private static boolean tryDir(String dir) {
		try {
			var okPl = StooqLoader.unzipNewPL(GPW_TARGET_DATA_DIR, dir);
			var okUS = StooqLoader.unzipNewUS(US_TARGET_DATA_DIR, dir);
			return okPl || okUS;
		} catch (IOException e) {
			AppState.setDataStatus("Unzip failed");
			throw new DataProcessingException(UNZIP_FAILED, e);
		}
	}

	private StooqDataProvider() {
	}
}

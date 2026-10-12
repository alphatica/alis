package com.alphatica.alis.studio.logic.data.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.alphatica.alis.data.loader.DataProcessingException.Reason.DATA_NOT_FOUND;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.LOAD_FAILED;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.NO_DATA;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.UNZIP_FAILED;
import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

/**
 * Market data downloaded from Stooq.pl. Failures are thrown as {@link DataProcessingException} with the reason.
 */
public final class StooqDataProvider {
	private static final String GPW_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_gpw";
	private static final String US_TARGET_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "stooq_us";

	private StooqDataProvider() {
	}

	public static MarketData loadUSData() {
		return load(() -> StooqLoader.loadUS(US_TARGET_DATA_DIR));
	}

	public static MarketData loadPLData() {
		return loadPLData(Path.of(GPW_TARGET_DATA_DIR));
	}

	public static MarketData loadPLData(Path dataDirectory) {
		return load(() -> StooqLoader.loadPL(dataDirectory.toString()));
	}

	@SuppressWarnings("java:S2142")
	private static MarketData load(Callable<MarketData> loader) {
		MarketData marketData;
		try {
			marketData = loader.call();
		} catch (Exception ex) {
			throw new DataProcessingException(LOAD_FAILED, ex);
		}
		if (marketData.listMarkets(ALL).isEmpty()) {
			throw new DataProcessingException(NO_DATA);
		}
		return marketData;
	}

	/**
	 * Unzips the newest Stooq archives from the Downloads directory into the work directory.
	 */
	public static void unzipNewData() {
		for (String directory : List.of("Pobrane", "Downloads")) {
			if (tryDir(directory)) {
				return;
			}
		}
		throw new DataProcessingException(DATA_NOT_FOUND);
	}

	private static boolean tryDir(String dir) {
		try {
			boolean okPl = StooqLoader.unzipNewPL(GPW_TARGET_DATA_DIR, dir);
			boolean okUs = StooqLoader.unzipNewUS(US_TARGET_DATA_DIR, dir);
			return okPl || okUs;
		} catch (IOException e) {
			throw new DataProcessingException(UNZIP_FAILED, e);
		}
	}
}

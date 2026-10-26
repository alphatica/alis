package com.alphatica.alis.studio.logic.riskfreerate;

import com.alphatica.alis.data.riskfreerate.RiskFreeRates;

import java.io.IOException;
import java.nio.file.Path;

import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

/**
 * Risk-free rates from the "rf" directory in the work directory. The files are created there on the first load,
 * so it must come after the user accepts the license.
 */
public final class RiskFreeRatesProvider {
	private static final Path DIRECTORY = Path.of(System.getProperty("user.home"), STUDIO_WORK_DIR, "rf");

	private static RiskFreeRates rates;

	private RiskFreeRatesProvider() {
	}

	/**
	 * Reads (and creates or extends) the rates files.
	 *
	 * @throws IOException when a file is invalid (the message names the file and the line) or cannot be written
	 */
	public static synchronized void load() throws IOException {
		rates = RiskFreeRates.load(DIRECTORY);
	}

	/**
	 * The loaded rates. The application loads them at the start, or after the user accepts the license; the API is
	 * closed until then.
	 */
	public static synchronized RiskFreeRates get() {
		if (rates == null) {
			throw new IllegalStateException("Risk-free rates are not loaded");
		}
		return rates;
	}
}

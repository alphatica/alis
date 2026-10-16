package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.loader.DataProcessingException.Reason.LOAD_FAILED;
import static com.alphatica.alis.data.loader.DataProcessingException.Reason.PROCESSING_FAILED;
import static com.alphatica.alis.tools.java.NumberTools.assertNotNegative;

final class DelistingLoader {

	static final String DELISTED_FILE = "delisted.csv";

	private DelistingLoader() {
	}

	static List<Delisting> load(Path file) {
		if (!Files.isRegularFile(file)) {
			return List.of();
		}
		try {
			return parse(Files.readAllLines(file));
		} catch (IOException e) {
			throw new DataProcessingException(LOAD_FAILED, e);
		}
	}

	static List<Delisting> parse(List<String> lines) {
		List<Delisting> delistings = new ArrayList<>(lines.size());
		for (int index = 1; index < lines.size(); index++) {
			try {
				delistings.add(parseLine(lines.get(index)));
			} catch (IllegalArgumentException e) {
				throw new DataProcessingException(PROCESSING_FAILED,
						new IllegalArgumentException("Invalid delisted line " + (index + 1) + ": " + e.getMessage(), e));
			}
		}
		return delistings;
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.err.println'
	static void warnAboutInconsistencies(List<Delisting> delistings, MarketData marketData) {
		for (Delisting delisting : delistings) {
			Market market = marketData.getMarket(delisting.market());
			if (market == null) {
				System.err.printf("Warning: delisted market %s (%s) not found in data%n", delisting.market(), delisting.time());
				continue;
			}
			long quotesAfter = market.getTimes().stream().filter(time -> time.isAfter(delisting.time())).count();
			if (quotesAfter > 0) {
				System.err.printf("Warning: market %s has %d quotes after delisting on %s%n", delisting.market(), quotesAfter,
						delisting.time());
			}
		}
	}

	private static Delisting parseLine(String line) {
		String[] fields = line.split(",", -1);
		if (fields.length != 3) {
			throw new IllegalArgumentException("expected 3 fields, got " + fields.length);
		}
		String time = fields[0].trim();
		String name = fields[1].trim();
		String price = fields[2].trim();
		if (name.isEmpty()) {
			throw new IllegalArgumentException("empty ticker");
		}
		return new Delisting(new MarketName(name), new Time(parseTime(time)), parsePrice(price));
	}

	private static int parseTime(String time) {
		try {
			return Integer.parseInt(time);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid date '" + time + "'", e);
		}
	}

	private static double parsePrice(String price) {
		double value;
		try {
			value = Double.parseDouble(price);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid price '" + price + "'", e);
		}
		// Zero price is allowed for delisting with nothing paid out
		assertNotNegative(value);
		return value;
	}
}

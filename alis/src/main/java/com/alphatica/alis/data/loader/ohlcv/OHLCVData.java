package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqMarket;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import static java.lang.String.format;

public class OHLCVData {
	private static final Layer[] LAYERS = Layer.values();
	MarketName name;
	List<OHLCVRow> rows;
	HashMap<Time, Integer> indexes;

	private OHLCVData() {
	}

	public static OHLCVData load(File file, int date, int open, int high, int low, int close, int vol, Time startTime) throws IOException {
		List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
		OHLCVData market = new OHLCVData();
		market.indexes = HashMap.newHashMap(4096);
		market.name = new MarketName(file.getName().split("\\.")[0]);
		market.rows = new ArrayList<>();
		if (lines.isEmpty()) {
			return market;
		}
		lines.removeFirst();
		for (String line : lines) {
			String[] parts = line.split(",");
			OHLCVRow quote = OHLCVRow.fromParts(parts[date], parts[open], parts[high], parts[low], parts[close], parts[vol]);
			if (quote.getTime().isBefore(startTime)) {
				continue;
			}
			if (!market.rows.isEmpty() && !quote.getTime().isAfter(market.rows.getLast().getTime())) {
				throw new IllegalArgumentException(format("Wrong date order or repeated date: %s, %d", file.getAbsolutePath(),
						quote.getTime().time()));
			}
			market.indexes.put(quote.getTime(), market.rows.size());
			market.rows.add(quote);
		}
		return market;
	}

	public Market toMarket(MarketType marketType) {
		int size = rows.size();
		int[] times = new int[size];
		float[][] columns = new float[LAYERS.length][size];
		for (int i = 0; i < size; i++) {
			OHLCVRow row = rows.get(i);
			times[i] = row.getTime().time();
			int newestFirst = size - 1 - i;
			for (Layer layer : LAYERS) {
				columns[layer.getIndex()][newestFirst] = row.get(layer);
			}
		}
		return new StooqMarket(getName(), marketType, times, columns);
	}

	public MarketName getName() {
		return name;
	}

	public void updateData(String path, int dateCol, int valueCol, BiConsumer<OHLCVRow, String> consumer) throws IOException {
		File file = new File(path);
		if (!file.exists()) {
			return;
		}
		List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
		if (lines.isEmpty()) {
			return;
		}
		lines.removeFirst();
		for (String line : lines) {
			String[] parts = line.split(",");
			Time date = new Time(Integer.parseInt(parts[dateCol]));
			final Optional<Integer> indexForDate = Optional.ofNullable(indexes.get(date));
			indexForDate.ifPresent(index -> consumer.accept(rows.get(index), parts[valueCol]));
		}
	}

	public List<OHLCVRow> getRows() {
		return rows;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OHLCVData stock = (OHLCVData) o;
		return name.equals(stock.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}

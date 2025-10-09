package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.loader.stooq.StooqMarket;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.FloatArraySlice;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.MV;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.layer.Layer.PB;
import static com.alphatica.alis.data.layer.Layer.PE;
import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static java.lang.String.format;

public class OHLCVData {
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
			if (!market.rows.isEmpty() && quote.getTime().isBefore(market.rows.getLast().getTime())) {
				throw new IllegalArgumentException(format("Wrong date order: %s, %d", file.getAbsolutePath(), quote.getTime().time()));
			}
			market.indexes.put(quote.getTime(), market.rows.size());
			market.rows.add(quote);
		}
		return market;
	}

	public Market toMarket(MarketType marketType) {
		List<OHLCVRow> reversed = getRows().reversed();
		int size = reversed.size();
		float[] open = new float[size];
		float[] high = new float[size];
		float[] low = new float[size];
		float[] close = new float[size];
		float[] turnover = new float[size];
		float[] pe = new float[size];
		float[] pb = new float[size];
		float[] mv = new float[size];
		TreeMap<Time, List<FloatArraySlice>> ranges = new TreeMap<>();
		int index = 0;
		for (OHLCVRow row : reversed) {
			open[index] = row.get(OPEN);
			high[index] = row.get(HIGH);
			low[index] = row.get(LOW);
			close[index] = row.get(CLOSE);
			turnover[index] = row.get(TURNOVER);
			pe[index] = row.get(PE);
			pb[index] = row.get(PB);
			mv[index] = row.get(MV);
			List<FloatArraySlice> sub = new ArrayList<>(8);
			sub.add(new FloatArraySlice(open, index));
			sub.add(new FloatArraySlice(high, index));
			sub.add(new FloatArraySlice(low, index));
			sub.add(new FloatArraySlice(close, index));
			sub.add(new FloatArraySlice(turnover, index));
			sub.add(new FloatArraySlice(pe, index));
			sub.add(new FloatArraySlice(pb, index));
			sub.add(new FloatArraySlice(mv, index));
			ranges.put(row.getTime(), sub);
			index++;
		}
		return new StooqMarket(ranges, getName(), marketType);
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

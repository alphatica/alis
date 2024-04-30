package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.market.MarketName;
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
    MarketName name;
    List<OHLCVRow> quotes;

    HashMap<Time, Integer> indexes;

    public static OHLCVData load(File file, int date, int open, int high, int low, int close, int vol) throws IOException {
        List<String> lines = FileUtils.readLines(file, Charset.defaultCharset());
        OHLCVData stock = new OHLCVData();
        stock.indexes = HashMap.newHashMap(4096);
        stock.name = new MarketName(file.getName().split("\\.")[0]);
        stock.quotes = new ArrayList<>();
        if (lines.isEmpty()) {
            return stock;
        }
        lines.removeFirst();
        for (String line : lines) {
            String[] parts = line.split(",");

            OHLCVRow quote = OHLCVRow.fromParts(parts[date], parts[open], parts[high], parts[low], parts[close], parts[vol]);
            if (!stock.quotes.isEmpty() && quote.getTime().isBefore(stock.quotes.getLast().getTime())) {
                throw new IllegalArgumentException(format("Wrong date order: %s, %d", file.getAbsolutePath(), quote.getTime().time()));
            }
            stock.indexes.put(quote.getTime(), stock.quotes.size());
            stock.quotes.add(quote);
        }
        return stock;
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
            indexForDate.ifPresent(index -> consumer.accept(quotes.get(index), parts[valueCol]));
        }
    }

    public List<OHLCVRow> getRows() {
        return quotes;
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

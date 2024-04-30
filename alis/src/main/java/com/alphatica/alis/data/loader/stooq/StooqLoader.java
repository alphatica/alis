package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.loader.ohlcv.OHLCVData;
import com.alphatica.alis.data.loader.ohlcv.OHLCVRow;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.DoubleArrayRange;
import com.alphatica.alis.tools.java.GreenThreadExecutor;
import com.alphatica.alis.tools.java.Zipper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.layer.Layer.PB;
import static com.alphatica.alis.data.layer.Layer.PE;
import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static java.io.File.separator;

public class StooqLoader {
    private StooqLoader() {
    }

    private static StooqMarket processFile(String dataDir, File file, String indicatorsPath, MarketType marketType) {

        try {
            OHLCVData ohlcv = OHLCVData.load(file, 2, 4, 5, 6, 7, 8);
            ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pb.txt", 2, 7, (q, s) -> q.parseAndSet(s, PB));
            ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pe.txt", 2, 7, (q, s) -> q.parseAndSet(s, PE));
            List<OHLCVRow> reversed = ohlcv.getRows().reversed();
            int size = reversed.size();
            double[] open = new double[size];
            double[] high = new double[size];
            double[] low = new double[size];
            double[] close = new double[size];
            double[] turnover = new double[size];
            double[] pb = new double[size];
            double[] pe = new double[size];
            TreeMap<Time, List<DoubleArrayRange>> ranges = new TreeMap<>();
            int index = 0;
            for (OHLCVRow row : reversed) {
                open[index] = row.get(OPEN);
                high[index] = row.get(HIGH);
                low[index] = row.get(LOW);
                close[index] = row.get(CLOSE);
                turnover[index] = row.get(TURNOVER);
                pb[index] = row.get(PB);
                pe[index] = row.get(PE);
                List<DoubleArrayRange> sub = new ArrayList<>(7);
                sub.add(new DoubleArrayRange(open, index, size));
                sub.add(new DoubleArrayRange(high, index, size));
                sub.add(new DoubleArrayRange(low, index, size));
                sub.add(new DoubleArrayRange(close, index, size));
                sub.add(new DoubleArrayRange(turnover, index, size));
                sub.add(new DoubleArrayRange(pb, index, size));
                sub.add(new DoubleArrayRange(pe, index, size));
                ranges.put(row.getTime(), sub);
                index++;
            }
            return new StooqMarket(ranges, ohlcv.getName(), marketType);
        } catch (IOException e) {
            throw new DataProcessingException(e);
        }
    }

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static StooqData load(String workDir) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        String dataDir = workDir + separator + "stooq_data" + separator + "data" + separator + "daily" + separator + "pl" + separator;
        StooqData stooqData = new StooqData();
        Map<MarketName, StooqMarket> stocks = loadFiles(dataDir, "wse stocks", "wse stocks indicators", MarketType.STOCK);
        stooqData.addMarkets(stocks);
        Map<MarketName, StooqMarket> indices = loadFiles(dataDir, "wse indices", "wse indices indicators", MarketType.INDICE);
        stooqData.addMarkets(indices);
        long endTime = System.currentTimeMillis();
        System.out.println("Data loaded in " + (endTime - startTime) + " ms");
        return stooqData;
    }

    public static Map<MarketName, StooqMarket> loadFiles(String dataDir, String filesPath, String indicatorsPath, MarketType marketType) throws ExecutionException, InterruptedException {
        File ohlcvDir = new File(dataDir + separator + filesPath);
        File[] files = ohlcvDir.listFiles();

        TreeMap<MarketName, StooqMarket> stocks = new TreeMap<>();
        if (files == null) {
            return stocks;
        }
        GreenThreadExecutor<File, StooqMarket> fileMarketGreenThreadExecutor = new GreenThreadExecutor<>(file -> processFile(dataDir, file, indicatorsPath, marketType));
        for (File file : files) {
            fileMarketGreenThreadExecutor.submit(file);
        }
        fileMarketGreenThreadExecutor.results().forEach(stock -> stocks.put(stock.getName(), stock));
        return stocks;
    }

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void unzipNew(String workDir, String downloadDir) throws IOException {
        String newPath = System.getProperty("user.home") + separator + downloadDir + separator + "d_pl_txt.zip";
        File newData = new File(newPath);
        if (newData.exists() && newData.isFile()) {
            long startTime = System.currentTimeMillis();
            System.out.println("Extracting new data...");
            Zipper.unzip(newPath, workDir + separator + "stooq_data");
            Files.delete(newData.toPath());
            long endTime = System.currentTimeMillis();
            System.out.println("Data extracted in " + (endTime - startTime) + " ms");
        }
    }

}


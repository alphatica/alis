package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.loader.ohlcv.OHLCVData;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.tools.java.TaskExecutor;
import com.alphatica.alis.tools.java.Zipper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.layer.Layer.MV;
import static com.alphatica.alis.data.layer.Layer.PB;
import static com.alphatica.alis.data.layer.Layer.PE;
import static java.io.File.separator;

public class StooqLoader {
	private StooqLoader() {
	}

	public static StooqData loadForex(String dataDir) throws ExecutionException, InterruptedException {
		StooqData stooqData = new StooqData();
		Map<MarketName, Market> stocks = loadForexFiles(dataDir, "forex");
		stooqData.addMarkets(stocks);
		return stooqData;
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static StooqData load(String workDir) throws ExecutionException, InterruptedException {
		String dataDir = workDir + separator + "stooq_data" + separator + "data" + separator + "daily" + separator + "pl" + separator;
		StooqData stooqData = new StooqData();
		Map<MarketName, Market> stocks = loadFiles(dataDir, "wse stocks", "wse stocks indicators", MarketType.STOCK);
		stooqData.addMarkets(stocks);
		Map<MarketName, Market> indices = loadFiles(dataDir, "wse indices", "wse indices indicators", MarketType.INDICE);
		stooqData.addMarkets(indices);
		return stooqData;
	}

	public static Map<MarketName, Market> loadFiles(String dataDir, String filesPath, String indicatorsPath, MarketType marketType) throws ExecutionException, InterruptedException {
		File ohlcvDir = new File(dataDir + separator + filesPath);
		File[] files = ohlcvDir.listFiles();

		TreeMap<MarketName, Market> stocks = new TreeMap<>();
		if (files == null) {
			return stocks;
		}
		TaskExecutor<Market> stooqMarketGreenLambdaExecutor = new TaskExecutor<>();
		for (File file : files) {
			stooqMarketGreenLambdaExecutor.submit(() -> processFile(dataDir, file, indicatorsPath, marketType));
		}
		stooqMarketGreenLambdaExecutor.getResults().forEach(stock -> stocks.put(stock.getName(), stock));
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

	public static Map<MarketName, Market> loadForexFiles(String dataDir, String filesPath) throws ExecutionException, InterruptedException {
		File ohlcvDir = new File(dataDir + separator + filesPath);
		File[] files = ohlcvDir.listFiles();

		TreeMap<MarketName, Market> stocks = new TreeMap<>();
		if (files == null) {
			return stocks;
		}
		TaskExecutor<Market> stooqMarketGreenLambdaExecutor = new TaskExecutor<>();
		for (File file : files) {
			stooqMarketGreenLambdaExecutor.submit(() -> processForexFile(file));
		}
		stooqMarketGreenLambdaExecutor.getResults().forEach(stock -> stocks.put(stock.getName(), stock));
		return stocks;
	}

	private static Market processForexFile(File file) {
		try {
			OHLCVData ohlcv = OHLCVData.load(file, 0, 1, 2, 3, 4);
			return ohlcv.toMarket(MarketType.FOREX);
		} catch (IOException e) {
			throw new DataProcessingException(e);
		}
	}

	private static Market processFile(String dataDir, File file, String indicatorsPath, MarketType marketType) {
		try {
			OHLCVData ohlcv = OHLCVData.load(file, 2, 4, 5, 6, 7, 8);
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pe.txt", 2, 7, (q, s) -> q.parseAndSet(s, PE));
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pb.txt", 2, 7, (q, s) -> q.parseAndSet(s, PB));
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_mv.txt", 2, 7, (q, s) -> q.parseAndSet(s, MV));
			return ohlcv.toMarket(marketType);
		} catch (IOException e) {
			throw new DataProcessingException(e);
		}
	}

}


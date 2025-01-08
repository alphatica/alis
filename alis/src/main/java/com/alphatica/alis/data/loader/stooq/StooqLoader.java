package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.StandardMarketData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.alphatica.alis.data.layer.Layer.MV;
import static com.alphatica.alis.data.layer.Layer.PB;
import static com.alphatica.alis.data.layer.Layer.PE;
import static com.alphatica.alis.data.market.MarketType.STOCK;
import static java.io.File.separator;

public class StooqLoader {

	private static final String STOOQ_DATA_DIR = "stooq_data";

	public static StandardMarketData loadUS(String workDir) {
		List<File> files = getStockFiles(workDir);
		StandardMarketData marketData = loadUSFiles(files);
		return marketData;
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static StandardMarketData loadPL(String workDir) throws ExecutionException, InterruptedException {
		String dataDir = workDir + separator + STOOQ_DATA_DIR + separator + "data" + separator + "daily" + separator + "pl" + separator;
		StandardMarketData standardMarketData = new StandardMarketData();
		Map<MarketName, Market> stocks = loadUSFiles(dataDir, "wse stocks", "wse stocks indicators", STOCK);
		standardMarketData.addMarkets(stocks);
		Map<MarketName, Market> indices = loadUSFiles(dataDir, "wse indices", "wse indices indicators", MarketType.INDICE);
		standardMarketData.addMarkets(indices);
		return standardMarketData;
	}

	public static boolean unzipNewPL(String workDir, String downloadDir) throws IOException {
		return unzipNew(workDir, downloadDir, "d_pl_txt.zip");
	}

	public static boolean unzipNewUS(String workDir, String downloadDir) throws IOException {
		return unzipNew(workDir, downloadDir, "d_us_txt.zip");
	}

	private static List<File> getStockFiles(String workDir) {
		List<File> stocks = new ArrayList<>();
		List<File> directories = new ArrayList<>(List.of(new File(workDir)));
		while(!directories.isEmpty()) {
			File checkingDir = directories.removeLast();
			File[] files = checkingDir.listFiles();
			if (files == null) {
				continue;
			}
			for(File file: files) {
				if (file.isDirectory()) {
					directories.add(file);
				} else if (file.getAbsolutePath().contains("stocks")){
					stocks.add(file);
				}
			}
		}
		return stocks;
	}

	private static boolean unzipNew(String workDir, String downloadDir, String file) throws IOException {
		String newPath = System.getProperty("user.home") + separator + downloadDir + separator + file;
		File newData = new File(newPath);
		if (newData.exists() && newData.isFile()) {
			Zipper.unzip(newPath, workDir + separator + STOOQ_DATA_DIR);
			Files.delete(newData.toPath());
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("java:S106")
	private static StandardMarketData loadUSFiles(List<File> files) {
		Map<MarketName, Market> map = new ConcurrentHashMap<>();

		try(ExecutorService executor = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors())) {
			for(File file: files) {
				executor.submit(() -> {
					try {
						Market market = OHLCVData.load(file, 2, 4, 5, 6, 7, 8).toMarket(STOCK);
						map.put(market.getName(), market);
					} catch (IOException e) {
						System.err.println("Exception: " + e);
					}
				});
			}
		}
		StandardMarketData marketData = new StandardMarketData();
		marketData.addMarkets(map);
		return marketData;
	}

	private static Map<MarketName, Market> loadUSFiles(String dataDir, String filesPath, String indicatorsPath, MarketType marketType) throws ExecutionException, InterruptedException {
		File ohlcvDir = new File(dataDir + separator + filesPath);
		File[] files = ohlcvDir.listFiles();

		TreeMap<MarketName, Market> stocks = new TreeMap<>();
		if (files == null) {
			return stocks;
		}
		TaskExecutor<Market> executor = new TaskExecutor<>();
		for (File file : files) {
			executor.submit(() -> processFile(dataDir, file, indicatorsPath, marketType));
		}
		executor.getResults().forEach(stock -> stocks.put(stock.getName(), stock));
		return stocks;
	}

	private static Market processFile(String dataDir, File file, String indicatorsPath, MarketType marketType) {
		try {
			OHLCVData ohlcv = OHLCVData.load(file, 2, 4, 5, 6, 7, 8);
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pe.txt", 2, 7, (q, s) -> q.parseAndSet(s,
					PE));
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_pb.txt", 2, 7, (q, s) -> q.parseAndSet(s,
					PB));
			ohlcv.updateData(dataDir + File.separator + indicatorsPath + separator + ohlcv.getName() + "_mv.txt", 2, 7, (q, s) -> q.parseAndSet(s,
					MV));
			return ohlcv.toMarket(marketType);
		} catch (IOException e) {
			throw new DataProcessingException(e);
		}
	}

	private StooqLoader() {
	}
}


package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.TaskExecutor;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class SmaWithMinMaxOptimization {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData marketData = StooqLoader.loadPL(WORK_DIR);
		TaskExecutor<StrategyExecutor> lambdaExecutor = new TaskExecutor<>();
		for (int smaLength = 5; smaLength <= 360; smaLength += 5) {
			for (int minMaxLength = 5; minMaxLength <= 360; minMaxLength += 5) {
				int s = smaLength;
				int m = minMaxLength;
				lambdaExecutor.submit(() -> SmaMinMaxCheck.check(marketData, s, m, new Time(2008_01_23), new Time(2018_01_23)));
			}
		}
		lambdaExecutor.getResults();
	}

}

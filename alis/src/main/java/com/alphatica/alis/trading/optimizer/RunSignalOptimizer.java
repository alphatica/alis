package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ArithmeticAverageProfitPerBarScoreCalculator;
import com.alphatica.alis.trading.signalcheck.tradesignal.BuyAthSellSmaSignalGenerator;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class RunSignalOptimizer {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException, OptimizerException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		var optimizer = new SignalOptimizer(BuyAthSellSmaSignalGenerator::new, stooqData, new Time(2015_01_01), new Time(2026_01_01),
				STOCKS, 0.01f, false, ParametersSelection.FULL_PERMUTATION, 100.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION, new ArithmeticAverageProfitPerBarScoreCalculator());
		optimizer.run();
	}
}

package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.SignalExecutor;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ArithmeticAverageProfitPerBarScoreCalculator;
import com.alphatica.alis.trading.signalcheck.tradesignal.BuyAthSellSmaSignalGenerator;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class RunSignalOptimizer {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException, OptimizerException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		Supplier<SignalExecutor> executorFactory = () -> new SignalExecutor()
				.withTimeRange(new Time(2015_01_01), new Time(2026_01_01))
				.withMarketFilter(STOCKS)
				.withCommissionRate(0.01f)
				.withSecondarySignals(false)
				.useCachedMarketData();
		var optimizer = new SignalOptimizer(BuyAthSellSmaSignalGenerator::new, stooqData, executorFactory,
				ArithmeticAverageProfitPerBarScoreCalculator::new, AllocationPolicy.STOP_ON_FIRST_REJECTION,
				ParametersSelection.FULL_PERMUTATION, 100.0);
		optimizer.run();
	}
}

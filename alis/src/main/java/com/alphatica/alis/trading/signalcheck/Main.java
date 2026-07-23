package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ArithmeticAverageProfitPerBarScoreCalculator;
import com.alphatica.alis.trading.signalcheck.tradesignal.BuyAthSellSmaSignalGenerator;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class Main {

    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);

		var signalExecutor = new SignalExecutor()
				.withTimeRange(new Time(20150101), new Time(20260101))
				.withMarketFilter(STOCKS)
				.withCommissionRate(0.01)
				.withSecondarySignals(true)
				.withVerbose(true);
		var execution = signalExecutor.execute(stooqData, BuyAthSellSmaSignalGenerator::new);
		var scores = new AllocationScorer().calculateScores(execution,
				List.of(10.0, 20.0, 50.0, 100.0), AllocationPolicy.STOP_ON_FIRST_REJECTION,
				new ArithmeticAverageProfitPerBarScoreCalculator());
		for (AllocationScore score : scores) {
			AllocationReplayResult replay = score.replayResult();
			System.out.printf("Allocation: %.1f, score: %.3f, accepted: %d, rejected: %d, utilization: %.2f%%%n",
					replay.maxAllocation(), score.score(), replay.acceptedTradeCount(), replay.rejectedTrades(),
					replay.averageUtilization() * 100.0);
		}
    }
}

package com.alphatica.alis.examples.donchian;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DonchianSingle {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNew(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		double initialCash = 65585.79;
		StrategyExecutor executor = new StrategyExecutor().withInitialCash(initialCash)
														  .withTimeRange(new Time(2018_01_23), new Time(2025_01_01));
		Account account = executor.execute(stooqData, new DonchianStrategy(47, 116, true, false));
		System.out.println(account.getPositionsHistory().getStats());
		System.out.printf("%.0f%n", account.getNAV());
	}
}

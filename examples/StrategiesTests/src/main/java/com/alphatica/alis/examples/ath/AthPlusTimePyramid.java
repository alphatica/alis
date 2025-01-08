package com.alphatica.alis.examples.ath;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.TradePrice;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class AthPlusTimePyramid {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException, AccountActionException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		StrategyExecutor executor = new StrategyExecutor().withInitialCash(100_000)
														  .withTimeRange(new Time(2004_01_01), new Time(2025_01_01))
														  .withTradePrice(TradePrice.AVERAGE)
														  .withCommissionRate(0.003)
														  .withLimitOrderSize(0.2);
		Account account = executor.execute(stooqData, new AthPlusTimePyramidStrategy());
		System.out.printf("Account value: %.0f%n", account.getNAV());
	}
}

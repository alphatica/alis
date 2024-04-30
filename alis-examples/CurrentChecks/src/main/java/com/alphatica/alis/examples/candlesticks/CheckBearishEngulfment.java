package com.alphatica.alis.examples.candlesticks;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.indicators.candlesticks.BearishEngulfment;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;


public class CheckBearishEngulfment {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		Condition bearishEngulfment = new BearishEngulfment();
		ChangeCheck changeCheck = ChangeCheck.condition(bearishEngulfment)
											 .marketFilter(STOCKS)
											 .windowLength(20)
											 .from(new Time(2014_01_01))
											 .withHigherThanMoves(List.of(5.0, 10.0, 15.0, 20.0));
		ChangeCheckResult results = ChangeCheckExecutor.execute(changeCheck, stooqData);
		results.removeOverlapping();
		results.showStats(System.out);
	}
}

package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class SmaMinMaxOOS {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException, AccountActionException {
		MarketData marketData = StooqLoader.loadPL(WORK_DIR);
		SmaMinMaxCheck.check(marketData, 300, 70, new Time(2018_01_23), new Time(2025_01_23));
	}
}

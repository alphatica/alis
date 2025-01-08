package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.datamining.MarketStateSet;

import java.util.concurrent.ThreadLocalRandom;

public class Sma implements BetterExitFinder {
	private final com.alphatica.alis.indicators.trend.Sma sma;
	private final int len;

	public static Sma generator() {
		return new Sma(ThreadLocalRandom.current().nextInt(5, 250));
	}

	@Override
	public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
		return marketData.getData(Layer.CLOSE, 0) < sma.calculate(marketData);
	}

	@Override
	public String name() {
		return Sma.class.getSimpleName() + " " + len;
	}

	@Override
	public String description() {
		return "Exit when close falls below SMA(" + len + ")";
	}

	private Sma(int len) {
		this.sma = new com.alphatica.alis.indicators.trend.Sma(len);
		this.len = len;
	}
}

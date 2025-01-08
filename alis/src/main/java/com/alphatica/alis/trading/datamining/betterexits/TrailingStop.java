package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.datamining.MarketStateSet;

import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static java.lang.String.format;

public class TrailingStop implements BetterExitFinder {
	private final int stopLoss;

	public static TrailingStop generator() {
		return new TrailingStop(ThreadLocalRandom.current().nextInt(1, 80));
	}

	@Override
	public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
		DoubleValueState highest = (DoubleValueState) marketStateSet.get(marketData.getMarketName(), DoubleValueState::new);
		double now = marketData.getData(Layer.CLOSE, 0);
		if (now > highest.value) {
			highest.value = now;
		}
		double change = percentChange(highest.value, now);
		return change < -stopLoss;
	}

	@Override
	public String name() {
		return TrailingStop.class.getSimpleName() + " " + format("%d", stopLoss);
	}

	@Override
	public String description() {
		return format("Exit when close falls more than %d%% from highest close", stopLoss);
	}

	private TrailingStop(int stopLoss) {
		this.stopLoss = stopLoss;
	}
}

package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;

import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static java.lang.String.format;

public class ExitIfSmallProfitAfter implements BetterExitFinder {
	private final int change;
	private final int bars;

	public static ExitIfSmallProfitAfter generate() {
		return new ExitIfSmallProfitAfter(
				ThreadLocalRandom.current().nextInt(-50, 100),
				ThreadLocalRandom.current().nextInt(1, 250)
		);
	}

	ExitIfSmallProfitAfter(int change, int bars) {
		this.change = change;
		this.bars = bars;
	}

	@Override
	public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
		DoubleValueState state = marketStateSet.get(marketData.getMarketName());
		state.value++;
		if (state.value >= this.bars) {
			double entryPrice = account.getPosition(marketData.getMarketName()).getEntryPrice();
			double changeNow = percentChange(entryPrice, marketData.getData(CLOSE, 0));
			return changeNow < change;
		} else {
			return false;
		}
	}

	@Override
	public String name() {
		return ExitIfSmallProfitAfter.class.getSimpleName() + " " + change + " " + bars;
	}

	@Override
	public String description() {
		return format("Exit when profit after %d bars is lower than %d%%", bars, change);
	}
}

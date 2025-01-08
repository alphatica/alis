package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.DoubleArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.datamining.MarketStateSet;

import java.util.concurrent.ThreadLocalRandom;

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

	private ExitIfSmallProfitAfter(int change, int bars) {
		this.change = change;
		this.bars = bars;
	}

	@Override
	public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
		DoubleValueState state = (DoubleValueState) marketStateSet.get(marketData.getMarketName(), DoubleValueState::new);
		state.value++;
		if (state.value > this.bars) {
			DoubleArraySlice closes = marketData.getLayer(Layer.CLOSE);
			double changeNow = percentChange(closes.get((int)Math.round(state.value)), closes.get(0));
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

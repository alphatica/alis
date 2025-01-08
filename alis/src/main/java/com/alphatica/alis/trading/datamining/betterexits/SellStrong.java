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

public class SellStrong implements BetterExitFinder {
	private final int change;
	private final int bars;

	public static SellStrong generate() {
		return new SellStrong(
				ThreadLocalRandom.current().nextInt(5, 500),
				ThreadLocalRandom.current().nextInt(1, 250)
		);
	}

	private SellStrong(int change, int bars) {
		this.change = change;
		this.bars = bars;
	}

	@Override
	public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
		DoubleArraySlice closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() <= bars) {
			return false;
		} else {
			double changeNow = percentChange(closes.get(bars), closes.get(0));
			return changeNow > change;
		}
	}

	@Override
	public String name() {
		return SellStrong.class.getSimpleName() + " " + change + " " + bars;
	}

	@Override
	public String description() {
		return format("Exit when close-to-close change in %d bars is higher than %d%%", bars, change);
	}
}

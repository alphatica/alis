package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.datamining.MarketStateSet;

import java.util.concurrent.ThreadLocalRandom;

public class DaysInPosition implements BetterExitFinder {
    private final int days;

	public static DaysInPosition generator() {
		return new DaysInPosition(ThreadLocalRandom.current().nextInt(5, 250));
	}

	@Override
    public boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet) {
        DoubleValueState now = (DoubleValueState) marketStateSet.get(marketData.getMarketName(), DoubleValueState::new);
        now.value++;
		return now.value > days;
    }

	@Override
	public String name() {
		return DaysInPosition.class.getSimpleName() + " " + days;
	}

	@Override
	public String description() {
		return "Exit when size held for longer than " + days + " bars";
	}

	private DaysInPosition(int days) {
		this.days = days;
	}
}

package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.datamining.MarketStateSet;

public interface BetterExitFinder {

    boolean shouldExit(Account account, TimeMarketData marketData, TimeMarketDataSet allData, MarketStateSet marketStateSet);

    String name();

    String description();
}

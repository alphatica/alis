package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.TradeStats;

public record ResultStats(double nav, double maxDD, double returnToMaxDownsideDD, int trades, double accuracy,
						  double expectancy, double profitFactor, double profitPerTrade, double profitableMarketsPercent) {

	public static ResultStats of(Account account) {
		TradeStats stats = account.getStats();
		long profitableMarkets = account.countProfitableMarkets();
		long allMarkets = profitableMarkets + account.countUnprofitableMarkets();
		return new ResultStats(account.getNAV(), account.getMaxDD(),
				ReturnRatios.returnToMaxDrawdown(account.getAccountHistory().getSessions().totalReturnPercent(),
						account.getMaxDownsideDD()),
				stats.trades(), stats.accuracy(), stats.expectancy(), stats.profitFactor(), stats.profitPerTrade(),
				100.0 * profitableMarkets / allMarkets);
	}
}

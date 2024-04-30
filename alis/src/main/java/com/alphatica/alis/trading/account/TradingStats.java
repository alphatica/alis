package com.alphatica.alis.trading.account;

import static java.lang.String.format;

public record TradingStats(double overallProfitPercent,
						   double accuracy,
						   double averageWinPercent, double averageLossPercent,
						   double profitFactor,
						   int trades
						   ) {

	@Override
	public String toString() {
		return format(" overall profit percent: %.1f%n trades: %d%n accuracy: %.1f%n average win percent: %.1f%n average loss percent: %.1f%n profit factor: %.2f%n",
				overallProfitPercent, trades, accuracy, averageWinPercent, averageLossPercent, profitFactor);
	}
}

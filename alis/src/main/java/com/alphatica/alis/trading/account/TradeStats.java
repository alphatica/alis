package com.alphatica.alis.trading.account;

public record TradeStats(double profitPerTrade, double accuracy, double averageWinPercent, double averageLossPercent, double profitFactor,
						 double expectancy, int trades) {

}

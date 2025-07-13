package com.alphatica.alis.studio.view.window.trading.strategies.optimize.resulttable;


import com.alphatica.alis.trading.optimizer.OptimizerScore;

public record ResultsTableRow(double NAV, double maxDD, int trades, double accuracy, double expectancy, double profitFactor, double profitPerTrade,
							  OptimizerScore score) {
}

package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public record PositionPricesRecord(MarketName marketName, Time entryTime, Time exitTime, double entry, double exit, int quantity, double lowestLow,
								   double highestHigh) {

	public double getEntryEfficiency() {
		return lowestLow / entry;
	}

	public double getExitEfficiency() {
		return exit / highestHigh;
	}

	public double getProfitCash() {
		return quantity * (exit - entry);
	}

	public double getProfitPercent() {
		return percentChange(entry, exit);
	}

	public double value() {
		return quantity * exit;
	}

}

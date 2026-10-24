package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

/**
 * One row of position history with averaged prices.
 * <p>
 * Closed part ({@code open == false}): FIFO-matched quantity, average buy price of matched lots, average sell price,
 * entry time of the first buy, exit time of the last sell, trade length from the first buy to the last sell.
 * <p>
 * Open part ({@code open == true}): remaining quantity, average price of remaining lots, last close as exit price,
 * entry time of the oldest remaining lot, no exit time, trade length since the oldest remaining lot.
 * <p>
 * Entry commissions are split between closed and open part proportionally to quantity; exit commissions belong to the
 * closed part. For a market they sum up to all commissions paid.
 */
public record PositionHistoryRecord(MarketName marketName, Time entryTime, Time exitTime, double entry, double exit, int quantity,
									double entryCommission, double exitCommission, int tradeLength, double entryEfficiency,
									double exitEfficiency, boolean open) {

	public double commission() {
		return entryCommission + exitCommission;
	}

	/**
	 * Profit without commissions.
	 */
	public double getProfitCash() {
		return quantity * (exit - entry);
	}

	/**
	 * Profit without commissions.
	 */
	public double getProfitPercent() {
		return percentChange(entry, exit);
	}

	/**
	 * Profit after commissions.
	 */
	public double getNetProfitCash() {
		return getProfitCash() - commission();
	}

	/**
	 * Profit after commissions: entry commission increases cost, exit commission decreases proceeds.
	 */
	public double getNetProfitPercent() {
		return percentChange(quantity * entry + entryCommission, quantity * exit - exitCommission);
	}

	public double value() {
		return quantity * exit;
	}
}

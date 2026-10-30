package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionHistoryRecord;
import com.alphatica.alis.trading.account.PositionPricesRecord;

import java.util.List;
import java.util.Map;

/**
 * Return on invested capital in percent per year, over all trades together:
 * {@code 365 * sum(profit) / sum(capital * days) * 100}. NAV plays no role.
 * <ul>
 *     <li>capital of a bought lot: its entry value (quantity * entry price), so the entry commission only lowers it,</li>
 *     <li>profit: net of the exit commission,</li>
 *     <li>days: calendar days from the buy to the sell of each lot, at least 1 (a lot sold in the session it was
 *     bought).</li>
 * </ul>
 * Positions still open are valued at the last close and held until the last session. 0 without trades.
 */
public final class ReturnOnInvestedCapital implements AccountScorer {
	private static final int DAYS_PER_YEAR = 365;

	@Override
	public double score(Account account, Map<String, Double> customStats) {
		List<Time> sessions = account.getAccountHistory().getSessions().times();
		if (sessions.isEmpty()) {
			return 0;
		}
		long lastDay = sessions.getLast().epochDay();
		double capitalDays = 0;
		for (PositionPricesRecord lot : account.getAllPricesRecords()) {
			// No exit time: still open, or closed with the account after the last session
			long exitDay = lot.exitTime() == null ? lastDay : lot.exitTime().epochDay();
			capitalDays += lot.quantity() * lot.entry() * Math.max(1, exitDay - lot.entryTime().epochDay());
		}
		double profit = 0;
		for (PositionHistoryRecord trade : account.getAllHistoryRecords()) {
			profit += trade.getProfitCash() - trade.exitCommission();
		}
		return Scores.finiteOrZero(DAYS_PER_YEAR * profit / capitalDays * 100);
	}
}

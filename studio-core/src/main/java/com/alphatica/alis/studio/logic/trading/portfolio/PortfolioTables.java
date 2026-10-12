package com.alphatica.alis.studio.logic.trading.portfolio;

import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.trading.account.PositionHistoryRecord;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.account.actions.Withdrawal;
import com.alphatica.alis.trading.order.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Rows of the portfolio tables, newest first.
 */
public final class PortfolioTables {

	public static final List<Column> ACTIONS_COLUMNS = List.of(Column.date("Time"), Column.text("Market"), Column.text("Type"),
			Column.number("Price", 2), Column.number("Quantity", 0), Column.number("Value", 2), Column.number("Commission", 2));
	public static final List<Column> HISTORY_COLUMNS = List.of(Column.text("Market"), Column.date("Buy time"),
			Column.number("Buy price", 2), Column.date("Sell time"), Column.number("Sell price", 2), Column.number("Quantity", 0),
			Column.number("Value", 2), Column.number("Profit (cash)", 2), Column.number("Profit (%)", 2),
			Column.number("Buy efficiency", 2), Column.number("Sell efficiency", 2));

	private PortfolioTables() {
	}

	public static List<Object[]> actionRows(List<AccountAction> actions) {
		List<Object[]> rows = new ArrayList<>(actions.size());
		for (AccountAction action : actions.reversed()) {
			int time = action.time().time();
			rows.add(switch (action.actionType()) {
				case Trade trade -> new Object[]{time, trade.marketName().name(), trade.direction().toString(), trade.price(),
						trade.quantity(), tradeValue(trade), trade.commission()};
				case Deposit(double cash) -> new Object[]{time, null, "Add cash", null, null, cash, null};
				case Withdrawal(double cash) -> new Object[]{time, null, "Withdraw cash", null, null, cash, null};
			});
		}
		return rows;
	}

	/** Cash paid for a purchase or received from a sale, commission included. */
	private static double tradeValue(Trade trade) {
		double value = trade.quantity() * trade.price();
		return trade.direction() == Direction.BUY ? value + trade.commission() : value - trade.commission();
	}

	/**
	 * Values that need market data are empty when {@code haveMarketData} is false, unless the position is closed.
	 */
	public static List<Object[]> historyRows(List<PositionHistoryRecord> records, boolean haveMarketData) {
		List<Object[]> rows = new ArrayList<>(records.size());
		for (PositionHistoryRecord r : records.reversed()) {
			boolean closedOrPriced = !r.open() || haveMarketData;
			rows.add(new Object[]{r.marketName().toString(), r.entryTime().time(), r.entry(),
					r.exitTime() == null ? null : r.exitTime().time(), showIf(closedOrPriced, r.exit()), r.quantity(),
					showIf(haveMarketData, r.value()), showIf(closedOrPriced, r.getProfitCash()),
					showIf(closedOrPriced, r.getProfitPercent()), showIf(haveMarketData, r.entryEfficiency()),
					showIf(haveMarketData, r.exitEfficiency())});
		}
		return rows;
	}

	private static Double showIf(boolean show, double value) {
		return show ? value : null;
	}
}

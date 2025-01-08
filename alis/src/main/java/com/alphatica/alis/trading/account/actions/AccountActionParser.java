package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.order.Direction;

public class AccountActionParser {

	private AccountActionParser() {
	}

	public static AccountAction fromCsv(String row) {
		String[] fields = row.split(",");
		try {
			Time time = new Time(Long.parseLong(fields[0]));
			return switch (fields[1]) {
				case "Trade" -> parseTrade(fields, time);
				case "Withdrawal" -> parseWithdrawal(fields, time);
				case "Deposit" -> parseDeposit(fields, time);
				default -> throw new IllegalStateException("Unexpected value: " + fields[1]);
			};
		} catch (Exception ex) {
			throw new IllegalArgumentException("Unable to parse AccountAction `" + row + "`: " + ex.getMessage());
		}
	}

	private static AccountAction parseDeposit(String[] fields, Time time) {
		double v = parsePositiveDouble(fields[2]);
		return new AccountAction(time, new Deposit(v));
	}

	private static AccountAction parseWithdrawal(String[] fields, Time time) {
		double v = parsePositiveDouble(fields[2]);
		return new AccountAction(time, new Withdrawal(v));
	}

	private static AccountAction parseTrade(String[] fields, Time time) {
		MarketName marketName = new MarketName(fields[2]);
		Direction direction = Direction.fromString(fields[3]);
		double price = parsePositiveDouble(fields[4]);
		int quantity = (int) Math.round(parsePositiveDouble(fields[5]));
		double commission = parsePositiveDouble(fields[6]);
		return new AccountAction(time, new Trade(marketName, direction, price, quantity, commission));
	}

	private static double parsePositiveDouble(String field) {
		double v = Double.parseDouble(field);
		if (v < 0.0) {
			throw new IllegalArgumentException("Value `" + field + "` must be a positive number");
		}
		return v;
	}
}

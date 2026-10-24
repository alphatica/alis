package com.alphatica.alis.trading.account;

import com.alphatica.alis.trading.account.actions.AccountAction;

import java.util.ArrayList;
import java.util.List;

public class AccountHistory {
	private final List<PositionPricesRecord> pricesRecords;
	private final List<AccountAction> accountActions;
	private final List<PositionHistoryRecord> historyRecords;
	private double paidCommissions;
	private double cashPaymentsBalance;

	public AccountHistory(double cash) {
		cashPaymentsBalance = cash;
		pricesRecords = new ArrayList<>(1024);
		accountActions = new ArrayList<>(1024);
		historyRecords = new ArrayList<>(1024);
	}

	public void addCommission(double commission) {
		paidCommissions += commission;
	}

	public double getPaidCommissions() {
		return paidCommissions;
	}

	public void recordCashPayment(double value) {
		cashPaymentsBalance += value;
	}

	public double getCashPayments() {
		return cashPaymentsBalance;
	}

	public List<PositionPricesRecord> getPricesRecords() {
		return new ArrayList<>(pricesRecords);
	}

	public void addRecords(List<PositionPricesRecord> positionPricesRecords) {
		pricesRecords.addAll(positionPricesRecords);
	}

	public void addHistoryRecord(PositionHistoryRecord historyRecord) {
		historyRecords.add(historyRecord);
	}

	/**
	 * Fully closed positions, one record per position cycle.
	 */
	public List<PositionHistoryRecord> getHistoryRecords() {
		return new ArrayList<>(historyRecords);
	}

	public void addAction(AccountAction accountAction) {
		accountActions.add(accountAction);
	}

	public List<AccountAction> getActions() {
		return new ArrayList<>(accountActions);
	}

}

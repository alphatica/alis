package com.alphatica.alis.trading.signalcheck;

public enum TradeStatus {
	OPEN(true), PENDING_OPEN(false), PENDING_CLOSE(true);

	private final boolean countProfit;

	TradeStatus(boolean countProfit) {
		this.countProfit = countProfit;
	}

	public boolean countProfit() {
		return this.countProfit;
	}
}

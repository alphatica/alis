package com.alphatica.alis.studio.logic.stats.edge;

final class EdgeHelp {
	static final String EXPOSURE_EDGE = "Whether holding cash paid off: falls avoided minus rises missed, against "
			+ "being fully invested in the index.";
	static final String SELECTION_EDGE = "How well you pick stocks: which ones, when, and how much of each.";
	static final String TIMELINE = "Index × exposure: the index held in the same share of the account as your stocks, "
			+ "the rest in cash. The lower chart shows how many percent it is ahead of the index (negative: behind). It "
			+ "rises when cash saved you from falls and drops when cash made you miss rises, so in a rising market it "
			+ "usually drops.";
	static final String ACCOUNT_TIMELINE = "Your account against Index × exposure: the same amount invested, but in "
			+ "the index instead of your stocks. The lower chart shows how many percent the account is ahead. It rises "
			+ "when your stocks do better than the index: which stocks, when you buy and sell, and position sizes, "
			+ "after commissions. Together with Exposure edge it gives how far the account is ahead of the index; the "
			+ "percents multiply: +50 % here and −20 % there make +20 %.";
	static final String HELD_STOCKS_SUBTITLE = "The average stock you hold against the index; position sizes and cash "
			+ "left out.";
	static final String HELD_STOCKS = "Each session: the average change of the stocks you hold, from open to open, "
			+ "against the index's change, all added up. Every stock counts the same whatever its position size, every "
			+ "session counts the same however many stocks you hold, and days without stocks add nothing. The chart "
			+ "shows how many percent the stocks you hold are ahead of the index. Before commissions.";
	static final String SIZE_EFFECT_SUBTITLE = "Your position sizes against equal positions in the same stocks.";
	static final String SIZE_EFFECT = "Each session: the change of the stocks you hold weighted by the value of each "
			+ "position, against the same stocks in equal positions (as in Held stocks vs index), from open to open, "
			+ "all added up. The chart shows how many percent your sizes made more than equal positions would "
			+ "(negative: less). Together with Held stocks vs index it gives how the stocks you hold, at your sizes, "
			+ "did against the index; the percents multiply. Before commissions.";

	private EdgeHelp() {
	}
}

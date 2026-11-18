package com.alphatica.alis.studio.logic.stats.edge;

final class EdgeHelp {
	static final String ALPHA_SUBTITLE = "Your excess return over the risk-free rate, in % per year: what the index "
			+ "gives at your beta, plus Jensen's alpha split into stock selection, timing and cash.";
	static final String ALPHA = "Jensen's alpha is your excess return over the risk-free rate minus what the index "
			+ "gives at your beta (Index × beta), with the betas as in the stats: one for every period of the "
			+ "risk-free rate. Stock selection: how much your stocks beat the index at the part of the account "
			+ "invested in each session. Timing: how much you gained by investing that part instead of keeping your "
			+ "average beta; positive when you are invested on rising days and in cash on falling ones. Cash: the "
			+ "risk-free return the cash part misses, as cash earns no interest. The invested part is taken at the "
			+ "start of each session. After commissions.";
	static final String SHARPE_TREYNOR_SUBTITLE = "Whether a higher excess return or a lower risk puts your ratios above "
			+ "the index's: all the risk (volatility) in the Sharpe ratio, the risk that moves with the index (beta) in "
			+ "the Treynor ratio.";
	static final String SHARPE_TREYNOR = "Both ratios divide the excess return over the risk-free rate by the risk: "
			+ "the Sharpe ratio by the volatility (the standard deviation of the excess returns, per year), the Treynor "
			+ "ratio by the beta. The index has beta 1, so its Treynor ratio is its excess return. Each difference is "
			+ "split into the effect of your excess return and the effect of your risk, each taken at the average of "
			+ "the other, so the order does not matter. When the beta helps and the volatility does not, much of your "
			+ "risk does not move with the index: the Treynor ratio leaves it out, the Sharpe ratio counts it. A low "
			+ "beta (much cash) makes the Treynor ratio high, as the excess return is divided by it. When the average "
			+ "excess return is negative, lower risk lowers the ratios. Sessions as for beta: those with an index "
			+ "quote.";
	static final String HELD_STOCKS_SUBTITLE = "The stocks you hold in equal positions against the index; position sizes "
			+ "and cash left out.";
	static final String HELD_STOCKS = "The stocks you hold, while you hold them, as if every position started at the "
			+ "same share of the account and was then held: it grows and shrinks with its price, and buying more or "
			+ "selling part of it does not change it. Each session from open to open against the index, all added up; "
			+ "days without stocks add nothing. The chart shows how many percent these positions are ahead of the "
			+ "index. Before commissions.";
	static final String SIZE_EFFECT_SUBTITLE = "Your position sizes against equal positions in the same stocks.";
	static final String SIZE_EFFECT = "Your positions (the value of the shares you hold) against equal positions in the "
			+ "same stocks (as in Held stocks vs index), each session from open to open, all added up. It changes with "
			+ "how much you buy, buy more and sell; positions of the same share of the account, bought and held, leave "
			+ "it at 0. The chart shows how many percent your sizes made more than equal positions would (negative: "
			+ "less). Together with Held stocks vs index it gives how the stocks you hold, at your sizes, did against "
			+ "the index; the percents multiply. Before commissions.";

	private EdgeHelp() {
	}
}

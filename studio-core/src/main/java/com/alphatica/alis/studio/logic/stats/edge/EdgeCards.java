package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.stats.edge.EdgeBridge.Step;
import com.alphatica.alis.studio.logic.stats.edge.EdgeChart.Scale;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.trading.account.attribution.AlphaAttribution;
import com.alphatica.alis.trading.account.attribution.HeldStocks;
import com.alphatica.alis.trading.account.attribution.SharpeAttribution;
import com.alphatica.alis.trading.account.attribution.TreynorAttribution;

import java.util.List;

final class EdgeCards {
	private static final String PERCENT = " %";
	private static final String ACCOUNT = "Account";
	private static final String INDEX = "Index";
	private static final String BETA = "Beta";
	private static final String SHARPE = "Sharpe";
	private static final String TREYNOR = "Treynor";
	private static final String EXCESS_RETURN = "Excess return";
	private static final String HELD_STOCKS = "Held stocks vs index";

	private EdgeCards() {
	}

	static EdgeCard alpha(AlphaAttribution alpha, double beta) {
		EdgeBridge bridge = EdgeBridge.of(null, 1, PERCENT,
				Step.total("Index × beta", alpha.indexPart()),
				Step.change("Stock selection", alpha.selection()),
				Step.change("Timing", alpha.timing()),
				Step.change("Cash", alpha.cash()),
				Step.total(ACCOUNT, alpha.excessReturn()));
		return new EdgeCard("Jensen's alpha", EdgeHelp.ALPHA_SUBTITLE, EdgeHelp.ALPHA, null, List.of(bridge),
				List.of(Stat.finite(Stat.percent("Jensen's alpha", alpha.alpha(), 1)),
						Stat.finite(Stat.number(BETA, beta, 2))),
				null);
	}

	static EdgeCard sharpeAndTreynor(SharpeAttribution sharpe, TreynorAttribution treynor, double beta) {
		EdgeTable table = new EdgeTable(
				List.of(Column.text(""), Column.percent("Excess / yr", 1), Column.percent("Volatility / yr", 1),
						Column.number(BETA, 2), Column.number(SHARPE, 2), Column.percent(TREYNOR, 1)),
				List.of(EdgeTable.row(ACCOUNT, sharpe.excessReturn(), sharpe.volatility(), beta, sharpe.account(),
								treynor.account()),
						EdgeTable.row(INDEX, sharpe.indexExcessReturn(), sharpe.indexVolatility(), 1, sharpe.index(),
								treynor.index())));
		EdgeBridge sharpeBridge = EdgeBridge.of("Sharpe ratio", 2, "",
				Step.total(INDEX, sharpe.index()),
				Step.change(EXCESS_RETURN, sharpe.returnEffect()),
				Step.change("Volatility", sharpe.volatilityEffect()),
				Step.total(ACCOUNT, sharpe.account()));
		EdgeBridge treynorBridge = EdgeBridge.of("Treynor ratio", 1, PERCENT,
				Step.total(INDEX, treynor.index()),
				Step.change(EXCESS_RETURN, treynor.returnEffect()),
				Step.change(BETA, treynor.betaEffect()),
				Step.total(ACCOUNT, treynor.account()));
		return new EdgeCard("Sharpe & Treynor", EdgeHelp.SHARPE_TREYNOR_SUBTITLE, EdgeHelp.SHARPE_TREYNOR, table,
				List.of(sharpeBridge, treynorBridge), List.of(), null);
	}

	static EdgeCard heldStocks(List<Integer> times, HeldStocks heldStocks) {
		return new EdgeCard(HELD_STOCKS, EdgeHelp.HELD_STOCKS_SUBTITLE, EdgeHelp.HELD_STOCKS, null, List.of(), List.of(),
				new EdgeChart(times, List.of(EdgeSeries.of(HELD_STOCKS, heldStocks.versusBenchmark())), Scale.RELATIVE));
	}

	static EdgeCard sizeEffect(List<Integer> times, HeldStocks heldStocks) {
		return new EdgeCard("Position size effect", EdgeHelp.SIZE_EFFECT_SUBTITLE, EdgeHelp.SIZE_EFFECT, null,
				List.of(), List.of(), new EdgeChart(times, List.of(EdgeSeries.of("Your sizes vs equal sizes",
				heldStocks.sizeEffect())), Scale.RELATIVE));
	}
}

package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.attribution.ExposedBenchmark;
import com.alphatica.alis.trading.account.attribution.HeldStocks;

import java.util.List;

public record EdgeTimeline(String title, String subtitle, String help, List<Integer> times, List<EdgeSeries> series,
						   EdgeSeries difference) {
	private static final String INDEX = "Index";
	private static final String EXPOSED = "Index × exposure";
	private static final String HELD_STOCKS = "Held stocks vs index";

	static List<EdgeTimeline> of(List<Time> sessions, double[] returns, double[] exposure, double[] benchmark,
								 HeldStocks heldStocks) {
		List<Integer> times = sessions.stream().map(Time::time).toList();
		ExposedBenchmark curves = ExposedBenchmark.of(exposure, benchmark);
		double[] account = ReturnRatios.cumulativePercent(returns);
		return List.of(
				new EdgeTimeline("Exposure edge", EdgeHelp.EXPOSURE_EDGE, EdgeHelp.TIMELINE, times,
						List.of(EdgeSeries.of(INDEX, curves.benchmark()), EdgeSeries.of(EXPOSED, curves.exposed())),
						EdgeSeries.of(EXPOSED + " vs index", curves.difference())),
				new EdgeTimeline("Selection edge", EdgeHelp.SELECTION_EDGE, EdgeHelp.ACCOUNT_TIMELINE, times,
						List.of(EdgeSeries.of(EXPOSED, curves.exposed()), EdgeSeries.of("Account", account)),
						EdgeSeries.of("Account vs " + EXPOSED, ExposedBenchmark.logRatio(account, curves.exposed()))),
				new EdgeTimeline(HELD_STOCKS, EdgeHelp.HELD_STOCKS_SUBTITLE, EdgeHelp.HELD_STOCKS, times,
						List.of(), EdgeSeries.of(HELD_STOCKS, heldStocks.versusBenchmark())),
				new EdgeTimeline("Position size effect", EdgeHelp.SIZE_EFFECT_SUBTITLE, EdgeHelp.SIZE_EFFECT, times,
						List.of(), EdgeSeries.of("Your sizes vs equal sizes", heldStocks.sizeEffect())));
	}
}

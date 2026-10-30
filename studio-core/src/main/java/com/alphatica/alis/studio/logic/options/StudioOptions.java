package com.alphatica.alis.studio.logic.options;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.studio.logic.riskfreerate.RiskFreeRatesProvider;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Cagr;
import com.alphatica.alis.trading.account.scorer.CalmarRatio;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.JensensAlpha;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;
import com.alphatica.alis.trading.account.scorer.ReturnOnInvestedCapital;
import com.alphatica.alis.trading.account.scorer.ReturnToMaxDownsideDD;
import com.alphatica.alis.trading.account.scorer.SharpeRatio;
import com.alphatica.alis.trading.account.scorer.SortinoRatio;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Choices offered in the GUI, by the name shown to the user, in the order shown. Scorers are {@link ScorerOption}s,
 * resolved once per job. Ratio scorers use the risk-free rates of {@link RiskFreeRatesProvider} (loaded before the API
 * opens) and the country of {@link RatioStats}; Jensen's alpha also its benchmark.
 */
public final class StudioOptions {

	public static final Map<String, ResultVerifier> RESULT_VERIFIERS = ordered(Map.entry("None", ResultVerifier.NONE),
			Map.entry("Remove markets", ResultVerifier.REMOVE_MARKETS),
			Map.entry("Remove orders", ResultVerifier.REMOVE_ORDERS),
			Map.entry("Fuzzy start time", ResultVerifier.FUZZY_START_TIME));

	public static final Map<String, ParametersSelection> PARAMETERS_SELECTIONS = parametersSelections();

	public static final Map<String, ScorerOption> SCORERS = ordered(
			Map.entry("Net asset value", ScorerOption.of(NetAssetValue::new)),
			Map.entry("CAGR", ScorerOption.of(Cagr::new)),
			Map.entry("Return on invested capital", ScorerOption.of(ReturnOnInvestedCapital::new)),
			Map.entry("Trade expectancy", ScorerOption.of(Expectancy::new)),
			Map.entry("NAV / max drawdown", ScorerOption.of(NavAdjustedForMaxDD::new)),
			Map.entry("Return / max downside DD", ScorerOption.of(ReturnToMaxDownsideDD::new)),
			Map.entry("Sharpe ratio", ScorerOption.of(() -> new SharpeRatio(RiskFreeRatesProvider.get(), RatioStats.COUNTRY))),
			Map.entry("Sortino ratio", ScorerOption.of(() -> new SortinoRatio(RiskFreeRatesProvider.get(), RatioStats.COUNTRY))),
			Map.entry("Calmar ratio", ScorerOption.of(CalmarRatio::new)),
			Map.entry("Jensen's alpha", StudioOptions::jensensAlpha));

	private StudioOptions() {
	}

	@SafeVarargs
	private static <T> Map<String, T> ordered(Map.Entry<String, T>... entries) {
		Map<String, T> map = new LinkedHashMap<>();
		for (Map.Entry<String, T> entry : entries) {
			map.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(map);
	}

	private static Map<String, ParametersSelection> parametersSelections() {
		Map<String, ParametersSelection> map = new LinkedHashMap<>();
		for (ParametersSelection selection : ParametersSelection.values()) {
			map.put(selection.getText(), selection);
		}
		return Collections.unmodifiableMap(map);
	}

	/** Against the whole history of {@link RatioStats#BENCHMARK} in the job's market data. */
	private static Supplier<AccountScorer> jensensAlpha(MarketData marketData) throws UnavailableScorerException {
		Market benchmark = marketData.getMarket(RatioStats.BENCHMARK);
		if (benchmark == null) {
			throw new UnavailableScorerException("Jensen's alpha needs the benchmark " + RatioStats.BENCHMARK.name()
					+ " in the loaded market data");
		}
		RiskFreeRates rates = RiskFreeRatesProvider.get();
		return () -> new JensensAlpha(rates, RatioStats.COUNTRY, benchmark);
	}
}

package com.alphatica.alis.studio.logic.options;

import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;
import com.alphatica.alis.trading.account.scorer.ProfitableMarkets;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Choices offered in the GUI, by the name shown to the user, in the order shown. Scorers are factories: a scorer may
 * keep state, so every run gets a new one.
 */
public final class StudioOptions {

	public static final Map<String, ResultVerifier> RESULT_VERIFIERS = ordered(Map.entry("None", ResultVerifier.NONE),
			Map.entry("Remove markets", ResultVerifier.REMOVE_MARKETS),
			Map.entry("Remove orders", ResultVerifier.REMOVE_ORDERS),
			Map.entry("Fuzzy start time", ResultVerifier.FUZZY_START_TIME));

	public static final Map<String, ParametersSelection> PARAMETERS_SELECTIONS = parametersSelections();

	public static final Map<String, Supplier<AccountScorer>> EXIT_SCORERS = ordered(
			Map.entry("Net asset value", NetAssetValue::new),
			Map.entry("Trade expectancy", Expectancy::new),
			Map.entry("NAV / max drawdown", NavAdjustedForMaxDD::new));

	public static final Map<String, Supplier<AccountScorer>> SCORERS = withProfitableMarkets();

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

	private static Map<String, Supplier<AccountScorer>> withProfitableMarkets() {
		Map<String, Supplier<AccountScorer>> map = new LinkedHashMap<>(EXIT_SCORERS);
		map.put("Max profitable markets", ProfitableMarkets::new);
		return Collections.unmodifiableMap(map);
	}
}

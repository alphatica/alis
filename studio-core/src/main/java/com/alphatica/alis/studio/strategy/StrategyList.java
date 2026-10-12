package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.random.RandomStrategy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Strategies offered in the GUI: factories by name, in the order shown.
 */
public final class StrategyList {

	public static final Map<String, Supplier<Strategy>> STRATEGIES = strategies();

	private StrategyList() {
	}

	private static Map<String, Supplier<Strategy>> strategies() {
		Map<String, Supplier<Strategy>> map = new LinkedHashMap<>();
		map.put("DonchianChannel", DonchianChannel::new);
		map.put("BuyATH", BuyATH::new);
		map.put("RandomStrategy", RandomStrategy::new);
		return Collections.unmodifiableMap(map);
	}
}

package com.alphatica.alis.studio.logic.options;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.trading.account.scorer.AccountScorer;

import java.util.function.Supplier;

/**
 * A scorer offered in the GUI. A job resolves it once, against the market data it runs on, so all runs of the job
 * score with the same data even when other data is loaded meanwhile.
 */
@FunctionalInterface
public interface ScorerOption {

	/**
	 * Scorer factory for one job; a scorer may keep state, so every run gets a new one.
	 *
	 * @throws UnavailableScorerException when the market data lacks what the scorer needs
	 */
	Supplier<AccountScorer> forData(MarketData marketData) throws UnavailableScorerException;

	/** A scorer that does not need the market data. */
	static ScorerOption of(Supplier<AccountScorer> factory) {
		return marketData -> factory;
	}
}

package com.alphatica.alis.data.market;

import java.util.Map;

/**
 * Named values describing a market, e.g. one row of a strategy's summary table ({@code Strategy.getSummaryTable()}).
 */
@FunctionalInterface
public interface MarketAttributes {

	/**
	 * @return values by column name
	 */
	Map<String, String> toAttributes();

}

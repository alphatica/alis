package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

@FunctionalInterface
public interface MarketCursor {
	TimeMarketData atOrPrevious(Time time);
}

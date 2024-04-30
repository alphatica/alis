package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;

public interface Market {
	MarketName getName();

	MarketType getType();

	List<Time> getTimes();

	TimeMarketData getAtOrPrevious(Time time);

	TimeMarketData getAtOrNext(Time time);

	TimeMarketData getAt(Time time);
}

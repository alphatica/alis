package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;

@FunctionalInterface
public interface SnapshotCursor {
	TimeMarketDataSet at(Time time);
}

package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalTradeDiscoveryTest {

	@Test
	void shouldOwnTradeLifecycleFromSignalToClose() {
		var marketData = new TestData("market1");
		var discovery = new SignalTradeDiscovery(marketData, 0.01, false, null, null, false);
		List<Time> executionTimes = marketData.getTimes().stream()
				.filter(time -> !time.isBefore(new Time(10)) && !time.isAfter(new Time(20)))
				.toList();

		for (int eventIndex = 0; eventIndex < executionTimes.size(); eventIndex++) {
			Time time = executionTimes.get(eventIndex);
			var snapshot = marketData.snapshotAt(time);
			List<TimeMarketData> markets = snapshot.listUpToDateMarkets(STOCKS);
			discovery.process(time, snapshot, markets, TestSignalGenerator::new, eventIndex);
		}
		discovery.closeLastTrades(executionTimes.size());

		assertEquals(1, discovery.opportunities().size());
		TradeOpportunity trade = discovery.opportunities().getFirst();
		assertEquals(new Time(12), trade.signalTime());
		assertEquals(new Time(13), trade.openTime());
		assertEquals(new Time(20), trade.closeTime());
		assertEquals(13.13f, trade.effectiveOpenPrice(), 0.0001f);
		assertEquals(19.8f, trade.effectiveClosePrice(), 0.0001f);
	}
}

package com.alphatica.alis.trading.signalcheck.tradesignal;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SignalGeneratorApiTest {
	@Test
	void everyBuiltInSignalShouldReturnAnOptionalBuySignal() {
		var data = new TestData("market");
		var snapshot = data.snapshotAt(new Time(300));
		var market = snapshot.get(new MarketName("market"));
		List<SignalGenerator> generators = List.of(
				new BuyAthSellSmaSignalGenerator(),
				new DonchianChannelSignalGenerator(),
				new MinMaxSignalGenerator(),
				new RandomSignalGenerator(),
				new SmaCrossSignalGenerator(),
				new SmaMinMaxSignalGenerator());

		for (SignalGenerator generator : generators) {
			assertNotNull(generator.shouldBuy(market, snapshot));
		}
	}
}

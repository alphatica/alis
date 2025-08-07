package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.MarketScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class TotalMarketWeightCalculatorTest {

//	@Test // expected time below 10 M nanos
	void perf() {
		long totalTime = 0;
		int ok = 0;
		for(int i = 0; i < 1000; i++) {
			var positionReports = new PositionReporter();
			var weightMap = new SourceWeightMap();
			weightMap.addWeight("s1", 7.0);
			weightMap.addWeight("s2", 11.0);
			weightMap.addWeight("s3", 5.0);
			weightMap.addWeight("s4", 3.0);
			weightMap.addWeight("s5", 1.0);
			for(int j = 0; j < 100000; j++) {
				var s = "s" + ThreadLocalRandom.current().nextInt(5) + 1;
				var m = new MarketName("m" + ThreadLocalRandom.current().nextInt(10));
				positionReports.report(new PositionReport(s, new Time(j % (250 * 10)), m, ThreadLocalRandom.current().nextFloat()));
			}
			var weightCalc = new TotalMarketWeightCalculator(weightMap, positionReports);
			long s = System.nanoTime();
			var r = weightCalc.getRanking(new Time(30));

			if (!r.isEmpty() && r.getFirst().value() > 0.1) {
				ok++;
			}
			long e = System.nanoTime();
			totalTime += (e -s);
		}
		System.out.println("Total time " + (totalTime) + " " + ok);
	}

	@Test
	void shouldCalculateMarketWeight() {
		var positionReports = new PositionReporter();
		positionReports.report(new PositionReport("s1", new Time(1), new MarketName("m1"), 1.0f));
		positionReports.report(new PositionReport("s1", new Time(1), new MarketName("m2"), 2.0f));
		positionReports.report(new PositionReport("s2", new Time(1), new MarketName("m1"), 3.0f));
		positionReports.report(new PositionReport("s2", new Time(1), new MarketName("m2"), 5.0f));
		positionReports.report(new PositionReport("s2", new Time(2), new MarketName("m2"), 13.0f));
		positionReports.report(new PositionReport("s3", new Time(1), new MarketName("m1"), 19.0f));
		positionReports.report(new PositionReport("s4", new Time(2), new MarketName("m1"), 23.0f));
		positionReports.report(new PositionReport("s4", new Time(1), new MarketName("m4"), 27.0f));
		positionReports.report(new PositionReport("s5", new Time(3), new MarketName("m4"), 31.0f));

		var weightMap = new SourceWeightMap();
		weightMap.addWeight("s1", 7.0);
		weightMap.addWeight("s2", 11.0);
		weightMap.addWeight("s3", 5.0);
		weightMap.addWeight("s4", 3.0);
		weightMap.addWeight("s5", 1.0);

		var weightCalc = new TotalMarketWeightCalculator(weightMap, positionReports);

		// position held (system weight * position size): 7*1 + 11*3
		assertEquals(135, weightCalc.totalMarketWeight(new Time(1), new MarketName("m1")));

		// position held (system weight * position size): 7*2 + 11*5
		assertEquals(69, weightCalc.totalMarketWeight(new Time(1), new MarketName("m2")));

		// non-existing market
		assertEquals(0, weightCalc.totalMarketWeight(new Time(1), new MarketName("m3")));

		// non-existing time
		assertEquals(0, weightCalc.totalMarketWeight(new Time(3), new MarketName("m1")));

		var ranking = weightCalc.getRanking(new Time(1));
		assertEquals(List.of(
				new MarketScore(new MarketName("m1"), 135),
				new MarketScore(new MarketName("m4"), 81),
				new MarketScore(new MarketName("m2"), 69)
				), ranking);
	}

	@Test
	void shouldReturnEmptyRankingForInvalidTime() {
		var positionReports = new PositionReporter();
		positionReports.report(new PositionReport("s1", new Time(1), new MarketName("m1"), 1.0f));
		positionReports.report(new PositionReport("s1", new Time(1), new MarketName("m2"), 2.0f));
		positionReports.report(new PositionReport("s2", new Time(1), new MarketName("m1"), 3.0f));
		positionReports.report(new PositionReport("s2", new Time(1), new MarketName("m2"), 5.0f));
		positionReports.report(new PositionReport("s2", new Time(2), new MarketName("m2"), 13.0f));

		var weightMap = new SourceWeightMap();
		weightMap.addWeight("s1", 7.0);
		weightMap.addWeight("s2", 11.0);

		var weightCalc = new TotalMarketWeightCalculator(weightMap, positionReports);
		assertEquals(emptyList(), weightCalc.getRanking(new Time(3)));
	}

}
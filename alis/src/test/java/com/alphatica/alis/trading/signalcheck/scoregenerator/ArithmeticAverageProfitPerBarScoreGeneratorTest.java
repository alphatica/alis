package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.OpenTrade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ArithmeticAverageProfitPerBarScoreGeneratorTest {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	@BeforeEach
	void setUp() {
		outputStreamCaptor.reset();
		System.setOut(new PrintStream(outputStreamCaptor));
	}

	@AfterEach
	void clean() {
		System.setOut(standardOut);
	}

	@Test
	void shouldCollectTradesAndCalculateProfit() {
		var scoreGenerator = new ArithmeticAverageProfitPerBarScoreGenerator();
		var t1 = new OpenTrade(null, 1.0f);
		t1.setOpenPrice(100.0f);
		t1.incrementBars();
		scoreGenerator.afterTrade(t1, 200.0f); // 100% profit in 1 bar
		var t2 = new OpenTrade(null, 2.0f);
		t2.setOpenPrice(20.0f);
		t2.incrementBars();
		t2.incrementBars();
		scoreGenerator.afterTrade(t2, 30.0f); // 50% profit in 2 bars
		var t3 = new OpenTrade(null, 0.5f);
		t3.setOpenPrice(30.0f);
		t3.incrementBars();
		scoreGenerator.afterTrade(t3, 15.0f); // 50% loss in 1 bar
		// Total profit 100 * 1 + 50 * 2 - 50 * 0.5 = 100 + 100 - 25 = 175% in 4 bars = 43.75
		assertEquals(43.75, scoreGenerator.score(), 0.01);
		scoreGenerator.show();
		assertEquals("""
				Total bars: 4
				Total trades: 3
				Average trade duration 1.33
				Average trade profit: 58.33333
				Average profit per bar: 43.750""", outputStreamCaptor.toString().trim());
	}

}
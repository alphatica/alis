package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;


class ChangeCheckTest {

	@Test
	void shouldCheckChange() throws ExecutionException, InterruptedException {
		MarketData data = new TestData("test_market");
		Condition condition = (market, allData) -> market.getTime().equals(new Time(99)) || market.getTime().equals(new Time(100));
		ChangeCheck check = ChangeCheck.condition(Condition.all(condition)).windowLength(10);
		ChangeCheckResult results = ChangeCheckExecutor.execute(check, data);
		Assertions.assertTrue(results.average().isPresent());
		double firstChange = percentChange(100, 110);
		double secondChange = percentChange(101, 111);
		Assertions.assertEquals((firstChange + secondChange) / 2, results.average().get(), 0.001);
		results.removeOverlapping();
		Assertions.assertEquals(firstChange, results.average().get(), 0.001);
	}
}

package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.loader.TestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;


class ChangeCheckTest {

	@Test
	void shouldCheckChange() throws ExecutionException, InterruptedException {
		MarketData data = new TestData();
		Condition condition = (market, allData) -> market.getTime().equals(new Time(0)) || market.getTime()
																								 .equals(new Time(1));
		ChangeCheck check = ChangeCheck.condition(Condition.all(condition)).windowLength(1);
		ChangeCheckResult results = ChangeCheckExecutor.execute(check, data);
		Assertions.assertTrue(results.average().isPresent());
		Assertions.assertEquals(-41.66, results.average().get(), 0.01);
		results.removeOverlapping();
		Assertions.assertEquals(-33.33, results.average().get(), 0.01);
	}
}

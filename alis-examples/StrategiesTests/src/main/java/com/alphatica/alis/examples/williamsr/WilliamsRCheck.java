package com.alphatica.alis.examples.williamsr;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.IndicatorAboveOrEqualLevel;
import com.alphatica.alis.condition.IndicatorBelowOrEqualLevel;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.indicators.oscilators.WilliamsR;
import com.alphatica.alis.tools.java.TaskExecutor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class WilliamsRCheck {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNew(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		Time start = new Time(2007_07_08);
		Time end = new Time(2025_01_01);
		for (double level = 0.05; level <= 1.01; level += 0.05) {
			for (int length = 10; length < 350; length += 10) {
				TaskExecutor<Double> objectGreenLambdaExecutor = new TaskExecutor<>();
				int len = length;
				double lvl = level;
				for (int window = 5; window <= 120; window += 5) {
					int w = window;
					objectGreenLambdaExecutor.submit(() -> {
						try {
							checkWilliamsRLevelCrossedUpwards(stooqData, start, end, len, lvl, w);
						} catch (ExecutionException | InterruptedException e) {
							throw new RuntimeException(e);
						}
						return 0.0;
					});
				}
				objectGreenLambdaExecutor.getResults();
			}
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void checkWilliamsRLevelCrossedUpwards(MarketData data, Time start, Time end, int length, double level, int window) throws ExecutionException, InterruptedException {
		Indicator williamsRCurrent = new WilliamsR(length);
		Indicator williamsRLast = new WilliamsR(length).withOffset(1);
		IndicatorAboveOrEqualLevel aboveCondition = new IndicatorAboveOrEqualLevel(williamsRCurrent, level);
		IndicatorBelowOrEqualLevel belowCondition = new IndicatorBelowOrEqualLevel(williamsRLast, level);
		Condition indicatorJustCrossed = Condition.all(aboveCondition, belowCondition);
		ChangeCheck changeCheck = ChangeCheck.condition(indicatorJustCrossed)
											 .from(start)
											 .to(end)
											 .windowLength(window)
											 .marketFilter(STOCKS);

		ChangeCheckResult results = ChangeCheckExecutor.execute(changeCheck, data);
		results.removeOverlapping();
		results.average().ifPresent(score -> System.out.printf("%2.1f,%d,%.2f,%d%n", score, length, level, window));


	}
}
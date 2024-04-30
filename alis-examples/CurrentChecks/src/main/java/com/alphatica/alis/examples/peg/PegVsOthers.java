package com.alphatica.alis.examples.peg;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.IndicatorAboveOrEqualLevel;
import com.alphatica.alis.condition.IndicatorBelowOrEqualLevel;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.indicators.PEG;
import com.alphatica.alis.indicators.trend.MinMax;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.condition.Condition.all;
import static com.alphatica.alis.condition.Condition.alwaysMatches;
import static com.alphatica.alis.condition.Condition.not;
import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class PegVsOthers {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNew(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		checkDate(stooqData, new Time(2014_01_02));
		checkDate(stooqData, new Time(2015_01_02));
		checkDate(stooqData, new Time(2016_01_04));
		checkDate(stooqData, new Time(2017_01_02));
		checkDate(stooqData, new Time(2018_01_03));
		checkDate(stooqData, new Time(2019_01_02));
		checkDate(stooqData, new Time(2020_01_02));
		checkDate(stooqData, new Time(2021_01_04));
		checkDate(stooqData, new Time(2022_01_03));
		checkDate(stooqData, new Time(2023_01_03));
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	static void checkDate(MarketData data, Time time) throws ExecutionException, InterruptedException {
		System.out.println("_________________________________________________________________");
		System.out.println("Checking for " + time);
		Condition pegBelow1 = new IndicatorBelowOrEqualLevel(new PEG(0, 250, 500, 750), 1.0);
		Condition upTrend = new IndicatorAboveOrEqualLevel(new MinMax(170), 0.0);

		checkConditionForTime(data, time, all(alwaysMatches(), upTrend), "All stocks in uptrend:");
		checkConditionForTime(data, time, all(pegBelow1, upTrend), "PEG below 1.0 and uptrend:");
		checkConditionForTime(data, time, all(not(pegBelow1), upTrend), "PEG above 1.0 and uptrend:");

		checkConditionForTime(data, time, all(alwaysMatches(), not(upTrend)), "All stocks in downtrend:");
		checkConditionForTime(data, time, all(pegBelow1, not(upTrend)), "PEG below 1.0 and downtrend:");
		checkConditionForTime(data, time, all(not(pegBelow1), not(upTrend)), "PEG above 1.0 and downtrend:");
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void checkConditionForTime(MarketData data, Time time, Condition condition, String message) throws ExecutionException, InterruptedException {
		System.out.println(message);
		ChangeCheck changeCheck = ChangeCheck.condition(condition)
											 .marketFilter(STOCKS)
											 .windowLength(250)
											 .from(time)
											 .to(time);
		ChangeCheckResult resultsAbove = ChangeCheckExecutor.execute(changeCheck, data);
		resultsAbove.average().ifPresent(v -> System.out.printf("%.1f%n", v));
	}
}

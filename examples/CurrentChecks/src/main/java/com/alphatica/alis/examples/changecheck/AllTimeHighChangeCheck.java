package com.alphatica.alis.examples.changecheck;


import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.HorizontalLine;
import com.alphatica.alis.charting.PaneSettings;
import com.alphatica.alis.charting.Scale;
import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.condition.TurnoverMoreThan;
import com.alphatica.alis.condition.changecheck.ChangeCheck;
import com.alphatica.alis.condition.changecheck.ChangeCheckExecutor;
import com.alphatica.alis.condition.changecheck.ChangeCheckResult;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class AllTimeHighChangeCheck {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		Condition condition = Condition.all(new AllTimeHigh(), new TurnoverMoreThan(50_000.0, 5));
		ChangeCheck changeCheck = ChangeCheck.condition(condition)
											 .marketFilter(STOCKS)
											 .windowLength(250)
											 .from(new Time(2015_01_01))
											 .withHigherThanMoves(List.of(30.0, 40.0, 50.0, 60.0, 80.0, 100.0, 150.0, 200.0));
		ChangeCheckResult results = ChangeCheckExecutor.execute(changeCheck, stooqData);
		results.showStats(System.out);

		results.removeOverlapping();
		System.out.println("After removing overlapping:");
		results.showStats(System.out);

		Chart<Time> chart = new Chart<>();
		List<HorizontalLine> horizontalLines = results.average()
				.map(average -> new HorizontalLine("Average", average))
				.stream()
				.toList();
		chart.addPane(
				Scale.LOGARITHMIC,
				"1-Year change after ATH",
				results.getChartLines(),
				new PaneSettings(1.0, "% Change", horizontalLines));
		chart.setCopyright("Alphatica.com");
		chart.setXName("Date");
		chart.createImage(new File("1yearAth.png"));
	}
}

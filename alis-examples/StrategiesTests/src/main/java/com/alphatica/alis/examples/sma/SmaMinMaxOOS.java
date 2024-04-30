package com.alphatica.alis.examples.sma;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SmaMinMaxOOS {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		MarketData marketData = StooqLoader.load(WORK_DIR);
		StrategyExecutor ex = SmaMinMaxCheck.check(marketData, 300, 70, new Time(2018_01_23), new Time(2025_01_23));
		Chart<Time> chart = new Chart<>();
		LineChartData<Time> benchmarkLine = ex.getBenchmarkLine();
		benchmarkLine.setName("WIG");
		LineChartData<Time> equityLine = ex.getEquityLine();
		equityLine.setName("Equity");
		chart.addDataLines(List.of(benchmarkLine, equityLine));
		chart.setCopyright("Alphatica.com");
		chart.setTitle("Sma/Min-Max System vs WIG Out-of-sample");
		chart.createImage(new File("sma-min-max-oos.png"));
	}
}

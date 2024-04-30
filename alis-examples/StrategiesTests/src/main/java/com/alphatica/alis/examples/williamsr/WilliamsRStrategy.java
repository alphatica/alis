package com.alphatica.alis.examples.williamsr;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.oscilators.WilliamsR;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.LONG;
import static com.alphatica.alis.trading.order.Direction.EXIT;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class WilliamsRStrategy extends Strategy {

	private static final boolean DRAW_CHART = true;

	private final double level;
	private final double position;
	private final WilliamsR williamsRNow;


	private final LineChartData<String> equityLine = new LineChartData<>();
	private final LineChartData<String> wigLine = new LineChartData<>();

	public WilliamsRStrategy(int length, double level, double position) {
		this.level = level;
		this.williamsRNow = new WilliamsR(length);
		this.position = position;
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		checkSells(data, account, orders);
		checkBuys(data, account, orders);
		if (DRAW_CHART) {
			addChartLinePoints(data, account);
		}
		return orders;
	}

	private void addChartLinePoints(TimeMarketDataSet data, Account account) {
		List<TimeMarketData> timeMarketData = data.listMarkets(STOCKS);
		if (!timeMarketData.isEmpty()) {
			String time = data.getTime().toString();
			equityLine.addPoint(time, account.getNAV());
			TimeMarketData wig = data.get(new MarketName("wig"));
			if (wig != null) {
				wigLine.addPoint(time, wig.getData(Layer.CLOSE, 0));
			}
		}
	}

	private void checkBuys(TimeMarketDataSet data, Account account, List<Order> orders) {
		if (account.getCash() >= account.getNAV() * position || !orders.isEmpty()) {
			for (TimeMarketData market : data.listMarkets(STOCKS)) {
				if (account.getPosition(market.getMarketName()) == null) {
					double wr = williamsRNow.calculate(market);
					if (wr >= level) {
						orders.add(new Order(market.getMarketName(), LONG, PROPORTION, position, wr));
					}
				}
			}
		}
	}

	private void checkSells(TimeMarketDataSet data, Account account, List<Order> orders) {
		for (TimeMarketData market : data.listMarkets(STOCKS)) {
			if (account.getPosition(market.getMarketName()) != null) {
				double wr = williamsRNow.calculate(market);
				if (wr < level) {
					orders.add(new Order(market.getMarketName(), EXIT, PROPORTION, 1.0, 0.0));
				}
			}
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	@Override
	public void finished(Account account) {
		if (DRAW_CHART) {
			Chart<String> chart = new Chart<>();
			equityLine.setConnectPoints(true);
			equityLine.setName("Equity");
			wigLine.setConnectPoints(true);
			wigLine.setName("WIG");
			chart.addDataLines(List.of(equityLine, wigLine));
			chart.setTitle("Williams R strategy");
			chart.setCopyright("Alphatica.com");
			try {
				chart.createImage(new File("williamsR.png"));
			} catch (IOException e) {
				System.out.println("Unable to create chart: " + e.getMessage());
			}
		}
	}
}

package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.trading.signals.SignalsProvider;
import com.alphatica.alis.studio.logic.trading.signals.StrategySummaryTable;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.trading.strategy.Strategy;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Strategies → Signals: orders a strategy would give, either for the portfolio (replaying its actions) or from a start date,
 * and the strategy's own summary of the markets afterwards.
 */
final class SignalsApi {
	private static final String PATH = "/api/signals";

	void register(RoutesConfig routes) {
		routes.get(PATH + "/options", ctx -> ctx.json(options()));
		routes.post(PATH, this::signals);
	}

	private static SignalsOptions options() {
		MarketData marketData = AppState.getMarketData();
		boolean haveTimes = marketData != null && !marketData.getTimes().isEmpty();
		String startTime = haveTimes ? marketData.getTimes().getLast().toString() : LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		return new SignalsOptions(List.copyOf(StrategyList.STRATEGIES.keySet()), startTime);
	}

	private void signals(Context ctx) throws Exception {
		SignalsRequest request = ctx.bodyAsClass(SignalsRequest.class);
		MarketData marketData = Inputs.marketData();
		Strategy strategy = Inputs.strategy(request.strategy()).get();
		List<Object[]> rows = request.forPortfolio()
				? SignalsProvider.forPortfolio(strategy, marketData)
				: SignalsProvider.fromTime(strategy, marketData, Inputs.time(request.startTime(), "start date"));
		StrategySummaryTable info = StrategySummaryTable.of(strategy.getSummaryTable());
		ctx.json(new SignalsView(new Table(SignalsProvider.COLUMNS, rows, "No signals."),
				new Table(info.columns(), info.rows(), "This strategy has no info.")));
	}

	/**
	 * @param defaultStartTime yyyymmdd: the last day of market data, or today when no data is loaded
	 */
	record SignalsOptions(List<String> strategies, String defaultStartTime) {
	}

	record SignalsRequest(String strategy, boolean forPortfolio, String startTime) {
	}

	record SignalsView(Table signals, Table info) {
	}
}

package com.alphatica.alis.studio.logic.stats;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.BenchmarkReturns;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.SessionHistory;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkRatios;

import java.util.List;

/**
 * Returns and risk-adjusted ratios of an account's session history, shared by the portfolio and backtests: with the
 * risk-free rate of {@link #COUNTRY} and against {@link #BENCHMARK}.
 */
public final class RatioStats {
	/** Benchmark of beta, Jensen's alpha and Treynor ratio. */
	public static final MarketName BENCHMARK = new MarketName("wig");
	/** Country of the risk-free rate. */
	public static final Country COUNTRY = Country.PL;

	private static final String TOTAL_RETURN = "Total return";
	private static final String CAGR = "CAGR";
	private static final String SHARPE = "Sharpe ratio";
	private static final String SORTINO = "Sortino ratio";
	private static final String CALMAR = "Calmar ratio";
	private static final String RETURN_TO_DOWNSIDE_DD = "Return / max downside DD";
	private static final String ALPHA = "Jensen's alpha";
	private static final String BETA = "Beta";
	private static final String TREYNOR = "Treynor ratio";

	private RatioStats() {
	}

	/** Time-weighted total return in percent, as on the equity chart; unknown without sessions. */
	public static Stat totalReturn(SessionHistory sessions) {
		return Stat.finite(Stat.percent(TOTAL_RETURN, sessions.totalReturnPercent(), 2));
	}

	/** Compound annual growth rate of the session returns; unknown without sessions. */
	public static Stat cagr(SessionHistory sessions) {
		return Stat.finite(Stat.percent(CAGR, ReturnRatios.cagrPercent(sessions.returns()), 2));
	}

	/**
	 * Sharpe, Sortino, Calmar, return / max downside DD, then Jensen's alpha, beta and Treynor against
	 * {@link #BENCHMARK}. Unknown values without sessions; benchmark ratios also without the benchmark in the market
	 * data (or without market data, {@code marketData == null}).
	 *
	 * @param maxDrawdownPercent         max drawdown in percent, 0 or negative
	 * @param maxDownsideDrawdownPercent max downside drawdown in percent, 0 or negative
	 */
	public static List<Stat> of(SessionHistory sessions, double maxDrawdownPercent, double maxDownsideDrawdownPercent,
								MarketData marketData, RiskFreeRates rates) {
		List<Time> times = sessions.times();
		if (times.isEmpty()) {
			return List.of(Stat.unknown(SHARPE), Stat.unknown(SORTINO), Stat.unknown(CALMAR),
					Stat.unknown(RETURN_TO_DOWNSIDE_DD), Stat.unknown(ALPHA), Stat.unknown(BETA), Stat.unknown(TREYNOR));
		}
		double[] returns = sessions.returns();
		List<RatePeriod> periods = rates.periods(COUNTRY, times.getFirst(), times.getLast());
		Market benchmarkMarket = marketData == null ? null : marketData.getMarket(BENCHMARK);
		double[] benchmark = benchmarkMarket == null ? null : BenchmarkReturns.at(benchmarkMarket, times);
		BenchmarkRatios benchmarkRatios = benchmark == null
				? new BenchmarkRatios(Double.NaN, Double.NaN, Double.NaN)
				: ReturnRatios.versusBenchmark(returns, benchmark, times, periods);
		return List.of(
				Stat.finite(Stat.number(SHARPE, ReturnRatios.sharpe(returns, times, periods), 2)),
				Stat.finite(Stat.number(SORTINO, ReturnRatios.sortino(returns, times, periods), 2)),
				Stat.finite(Stat.number(CALMAR, ReturnRatios.calmar(returns, maxDrawdownPercent), 2)),
				Stat.finite(Stat.number(RETURN_TO_DOWNSIDE_DD, ReturnRatios.returnToMaxDrawdown(
						sessions.totalReturnPercent(), maxDownsideDrawdownPercent), 2)),
				Stat.finite(Stat.percent(ALPHA, benchmarkRatios.jensenAlphaPercent(), 1)),
				Stat.finite(Stat.number(BETA, benchmarkRatios.beta(), 2)),
				Stat.finite(Stat.percent(TREYNOR, benchmarkRatios.treynorPercent(), 1)));
	}
}

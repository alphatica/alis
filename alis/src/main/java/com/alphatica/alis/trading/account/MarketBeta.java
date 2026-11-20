package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;

import java.util.List;

/** Beta of a market against a benchmark: {@code cov(r, rm) / var(rm)} of their session returns. */
public final class MarketBeta {
	/** Sessions of a beta: about a year. */
	private static final int SESSIONS = ReturnRatios.SESSIONS_PER_YEAR;
	/** Fewest sessions with returns of both markets for a beta. */
	public static final int MIN_SESSIONS = 20;

	private MarketBeta() {
	}

	/**
	 * Beta over the benchmark's last {@link #SESSIONS} sessions up to {@code end} (all of them when there are fewer),
	 * from the returns ({@link BenchmarkReturns#at}) at the sessions when both markets have one; NaN under
	 * {@link #MIN_SESSIONS} such sessions or when the benchmark's return does not change.
	 */
	public static double of(Market market, Market benchmark, Time end) {
		List<Time> sessions = lastSessions(benchmark, end);
		double[] returns = BenchmarkReturns.at(market, sessions);
		double[] benchmarkReturns = BenchmarkReturns.at(benchmark, sessions);
		int count = 0;
		double mean = 0;
		double meanBenchmark = 0;
		double coMoment = 0;
		double benchmarkMoment = 0;
		for (int i = 0; i < returns.length; i++) {
			double r = returns[i];
			double rm = benchmarkReturns[i];
			if (Double.isNaN(r) || Double.isNaN(rm)) {
				continue;
			}
			count++;
			double delta = r - mean;
			double deltaBenchmark = rm - meanBenchmark;
			mean += delta / count;
			meanBenchmark += deltaBenchmark / count;
			coMoment += delta * (rm - meanBenchmark);
			benchmarkMoment += deltaBenchmark * (rm - meanBenchmark);
		}
		return count < MIN_SESSIONS || benchmarkMoment <= 0 ? Double.NaN : coMoment / benchmarkMoment;
	}

	private static List<Time> lastSessions(Market benchmark, Time end) {
		List<Time> times = benchmark.getTimes();
		int to = times.size();
		while (to > 0 && times.get(to - 1).isAfter(end)) {
			to--;
		}
		return times.subList(Math.max(0, to - SESSIONS), to);
	}
}

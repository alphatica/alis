package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk-adjusted return ratios of a series of session returns (fractions, e.g. 0.01 = 1 %). Annualized with
 * {@link #SESSIONS_PER_YEAR}. Every method returns NaN when the ratio is undefined (too few sessions, no volatility,
 * no drawdown).
 * <p>
 * Ratios with the risk-free rate use excess returns: a session's return minus the risk-free return of the session,
 * {@code rf = rate * calendar days since the previous session / 365}, with the rate in force at the previous session
 * (the start of the session's return). The first session counts as one day at its own day's rate.
 */
public final class ReturnRatios {
	public static final int SESSIONS_PER_YEAR = 252;
	private static final int DAYS_PER_YEAR = 365;

	private ReturnRatios() {
	}

	/**
	 * Mean excess return over its sample standard deviation, annualized.
	 *
	 * @param times   session of every return, in increasing order
	 * @param periods risk-free rates covering the sessions, e.g. from the first to the last session
	 */
	public static double sharpe(double[] returns, List<Time> times, List<RatePeriod> periods) {
		RiskFreeCursor riskFree = new RiskFreeCursor(returns.length, times, periods);
		if (returns.length < 2) {
			return Double.NaN;
		}
		double mean = 0;
		double sumSquares = 0;
		for (int i = 0; i < returns.length; i++) {
			double excess = returns[i] - riskFree.next(i);
			double delta = excess - mean;
			mean += delta / (i + 1);
			sumSquares += delta * (excess - mean);
		}
		return ratio(mean, Math.sqrt(sumSquares / (returns.length - 1)));
	}

	/**
	 * Mean excess return over the downside deviation (root mean square of the negative excess returns, counted over
	 * all sessions), annualized. Arguments as in {@link #sharpe}.
	 */
	public static double sortino(double[] returns, List<Time> times, List<RatePeriod> periods) {
		RiskFreeCursor riskFree = new RiskFreeCursor(returns.length, times, periods);
		if (returns.length < 2) {
			return Double.NaN;
		}
		double sum = 0;
		double sumSquares = 0;
		for (int i = 0; i < returns.length; i++) {
			double excess = returns[i] - riskFree.next(i);
			sum += excess;
			if (excess < 0) {
				sumSquares += excess * excess;
			}
		}
		return ratio(sum / returns.length, Math.sqrt(sumSquares / returns.length));
	}

	/**
	 * Beta, Jensen's alpha and Treynor ratio against a benchmark, with the beta estimated separately in every period
	 * of one risk-free rate:
	 * <ul>
	 *     <li>{@code beta_k = cov(r - rf, rm - rf) / var(rm - rf)} in period k,</li>
	 *     <li>{@code alpha_k = mean(r - [rf + beta_k * (rm - rf)])} in period k,</li>
	 *     <li>beta and alpha: means of the periods' values weighted by their sessions; alpha annualized,</li>
	 *     <li>Treynor: annualized mean excess return over that beta.</li>
	 * </ul>
	 * A period without a beta (under 2 sessions, or the benchmark's excess return does not change) is joined to the
	 * previous period; the first such periods to the next one. Sessions where either return is NaN (e.g. no benchmark
	 * quote) are left out. Arguments as in {@link #sharpe}.
	 */
	public static BenchmarkRatios versusBenchmark(double[] returns, double[] benchmark, List<Time> times,
												  List<RatePeriod> periods) {
		if (returns.length != benchmark.length) {
			throw new IllegalArgumentException("returns and benchmark must have the same length");
		}
		RiskFreeCursor riskFree = new RiskFreeCursor(returns.length, times, periods);
		List<Moments> groups = new ArrayList<>();
		int groupPeriod = -1;
		for (int i = 0; i < returns.length; i++) {
			double rf = riskFree.next(i);
			if (riskFree.period() != groupPeriod) {
				groups.add(new Moments());
				groupPeriod = riskFree.period();
			}
			if (!Double.isNaN(returns[i]) && !Double.isNaN(benchmark[i])) {
				groups.getLast().add(returns[i] - rf, benchmark[i] - rf);
			}
		}
		return BenchmarkRatios.of(joinPeriodsWithoutBeta(groups));
	}

	/**
	 * Compound annual growth rate in percent, taking a year as {@link #SESSIONS_PER_YEAR} sessions.
	 */
	public static double cagrPercent(double[] returns) {
		if (returns.length == 0) {
			return Double.NaN;
		}
		double growth = 1;
		for (double r : returns) {
			growth *= 1 + r;
		}
		return (Math.pow(growth, (double) SESSIONS_PER_YEAR / returns.length) - 1) * 100;
	}

	/**
	 * CAGR over the max drawdown.
	 *
	 * @param maxDrawdownPercent max drawdown in percent, 0 or negative
	 */
	public static double calmar(double[] returns, double maxDrawdownPercent) {
		if (maxDrawdownPercent >= 0) {
			return Double.NaN;
		}
		return cagrPercent(returns) / -maxDrawdownPercent;
	}

	/**
	 * Total return over the max drawdown, e.g. with the max downside drawdown.
	 *
	 * @param totalReturnPercent total return in percent
	 * @param maxDrawdownPercent max drawdown in percent, 0 or negative
	 */
	public static double returnToMaxDrawdown(double totalReturnPercent, double maxDrawdownPercent) {
		if (maxDrawdownPercent >= 0) {
			return Double.NaN;
		}
		return totalReturnPercent / -maxDrawdownPercent;
	}

	public static double[] cumulativePercent(double[] returns) {
		double[] result = new double[returns.length];
		double growth = 1;
		for (int i = 0; i < returns.length; i++) {
			growth *= 1 + returns[i];
			result[i] = (growth - 1) * 100;
		}
		return result;
	}

	private static List<Moments> joinPeriodsWithoutBeta(List<Moments> groups) {
		List<Moments> result = new ArrayList<>();
		Moments leading = new Moments();
		for (Moments group : groups) {
			if (group.hasBeta()) {
				if (result.isEmpty()) {
					group.add(leading);
				}
				result.add(group);
			} else if (result.isEmpty()) {
				leading.add(group);
			} else {
				result.getLast().add(group);
			}
		}
		return result;
	}

	private static double ratio(double meanReturn, double deviation) {
		if (deviation == 0) {
			return Double.NaN;
		}
		return meanReturn / deviation * Math.sqrt(SESSIONS_PER_YEAR);
	}

	/**
	 * @param beta               mean of the periods' betas weighted by their sessions
	 * @param jensenAlphaPercent percent per year
	 * @param treynorPercent     mean excess return in percent per year over the beta
	 */
	public record BenchmarkRatios(double beta, double jensenAlphaPercent, double treynorPercent) {

		private static BenchmarkRatios of(List<Moments> periods) {
			int sessions = periods.stream().mapToInt(period -> period.count).sum();
			if (sessions == 0) {
				return new BenchmarkRatios(Double.NaN, Double.NaN, Double.NaN);
			}
			double beta = 0;
			double alpha = 0;
			double meanExcess = 0;
			for (Moments period : periods) {
				double weight = (double) period.count / sessions;
				double periodBeta = period.beta();
				beta += weight * periodBeta;
				alpha += weight * (period.meanReturn - periodBeta * period.meanBenchmark);
				meanExcess += weight * period.meanReturn;
			}
			double treynor = beta == 0 ? Double.NaN : meanExcess * SESSIONS_PER_YEAR * 100 / beta;
			return new BenchmarkRatios(beta, alpha * SESSIONS_PER_YEAR * 100, treynor);
		}
	}

	/** Risk-free return of each session in turn: sessions are visited in order, with the periods. */
	private static final class RiskFreeCursor {
		private final List<Time> times;
		private final List<RatePeriod> periods;
		private int period;
		private long previousDay;

		RiskFreeCursor(int sessions, List<Time> times, List<RatePeriod> periods) {
			if (times.size() != sessions) {
				throw new IllegalArgumentException("returns and times must have the same length");
			}
			this.times = times;
			this.periods = periods;
		}

		/** Index of the period of the last session. */
		int period() {
			return period;
		}

		double next(int session) {
			Time time = times.get(session);
			long day = time.epochDay();
			Time start = session == 0 ? time : times.get(session - 1);
			long days = session == 0 ? 1 : day - previousDay;
			if (days <= 0) {
				throw new IllegalArgumentException("Times must increase: " + time + " after " + start);
			}
			previousDay = day;
			return rateAt(start) * days / DAYS_PER_YEAR;
		}

		/** Moves to the period of the day: days are visited in order. */
		private double rateAt(Time day) {
			while (period < periods.size() && periods.get(period).to().isBefore(day)) {
				period++;
			}
			if (period == periods.size() || periods.get(period).from().isAfter(day)) {
				throw new IllegalArgumentException("No risk-free rate for " + day);
			}
			return periods.get(period).rate();
		}
	}

	/** Running means and co-moments of excess returns of the account and the benchmark (Welford). */
	private static final class Moments {
		private int count;
		private double meanReturn;
		private double meanBenchmark;
		private double coMoment;
		private double benchmarkMoment;

		void add(double excessReturn, double excessBenchmark) {
			count++;
			double deltaReturn = excessReturn - meanReturn;
			double deltaBenchmark = excessBenchmark - meanBenchmark;
			meanReturn += deltaReturn / count;
			meanBenchmark += deltaBenchmark / count;
			coMoment += deltaReturn * (excessBenchmark - meanBenchmark);
			benchmarkMoment += deltaBenchmark * (excessBenchmark - meanBenchmark);
		}

		/** Adds the sessions of the other moments, as if they were added one by one. */
		void add(Moments other) {
			if (other.count == 0) {
				return;
			}
			int total = count + other.count;
			double deltaReturn = other.meanReturn - meanReturn;
			double deltaBenchmark = other.meanBenchmark - meanBenchmark;
			double factor = (double) count * other.count / total;
			coMoment += other.coMoment + deltaReturn * deltaBenchmark * factor;
			benchmarkMoment += other.benchmarkMoment + deltaBenchmark * deltaBenchmark * factor;
			meanReturn += deltaReturn * other.count / total;
			meanBenchmark += deltaBenchmark * other.count / total;
			count = total;
		}

		boolean hasBeta() {
			return count >= 2 && benchmarkMoment > 0;
		}

		double beta() {
			return coMoment / benchmarkMoment;
		}
	}
}

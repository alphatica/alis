package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

/** Returns of a benchmark at the sessions of an account, for {@link ReturnRatios#versusBenchmark}. */
public final class BenchmarkReturns {
	private BenchmarkReturns() {
	}

	/**
	 * The benchmark's return at every session: its close over its previous close; NaN for a session it has no quote
	 * at (or no previous quote).
	 *
	 * @param times sessions, e.g. {@link SessionHistory#times()}
	 */
	public static double[] at(Market benchmark, List<Time> times) {
		double[] result = new double[times.size()];
		for (int i = 0; i < result.length; i++) {
			TimeMarketData data = benchmark.getAt(times.get(i));
			result[i] = data == null || data.getLayer(CLOSE).size() < 2
					? Double.NaN
					: (double) data.getData(CLOSE, 0) / data.getData(CLOSE, 1) - 1;
		}
		return result;
	}
}

package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.trading.account.ReturnRatios;

public record ExposedBenchmark(double[] benchmark, double[] exposed, double[] difference) {

	public static ExposedBenchmark of(double[] exposure, double[] benchmarkReturns) {
		double[] exposedReturns = new double[benchmarkReturns.length];
		for (int i = 1; i < exposedReturns.length; i++) {
			exposedReturns[i] = exposure[i - 1] * benchmarkReturns[i];
		}
		double[] benchmark = ReturnRatios.cumulativePercent(benchmarkReturns);
		double[] exposed = ReturnRatios.cumulativePercent(exposedReturns);
		return new ExposedBenchmark(benchmark, exposed, logRatio(exposed, benchmark));
	}

	public static double[] logRatio(double[] curvePercent, double[] referencePercent) {
		double[] result = new double[curvePercent.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = 100 * Math.log((100 + curvePercent[i]) / (100 + referencePercent[i]));
		}
		return result;
	}
}

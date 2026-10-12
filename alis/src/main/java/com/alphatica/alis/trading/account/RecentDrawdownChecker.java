package com.alphatica.alis.trading.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class RecentDrawdownChecker {

	/**
	 * @return the analysis as lines of text
	 */
	public static List<String> checkDrawdown(List<Double> navHistory) {
		List<String> lines = new ArrayList<>();
		if (navHistory.size() < 10) {
			lines.add(format("Need at least 10 history points, got %d", navHistory.size()));
		} else {
			doAnalysis(navHistory, lines);
		}
		return lines;
	}

	private static void doAnalysis(List<Double> navHistory, List<String> lines) {
		lines.add(format("Getting account history from %d data points", navHistory.size()));
		DrawdownCalc drawdownCalc = new DrawdownCalc();
		List<Double> ddLengths = new ArrayList<>();
		List<Double> ddDepths = new ArrayList<>();
		double currentDDLen = 0;
		double maxCurrentDdDepth = 0.0;
		for (Double nav : navHistory) {
			drawdownCalc.updateNav(nav);
			if (drawdownCalc.getCurrentDD() == 0 && currentDDLen > 0) {
				ddLengths.add(currentDDLen);
				currentDDLen = 0;
				ddDepths.add(maxCurrentDdDepth);
				maxCurrentDdDepth = 0;
			} else {
				currentDDLen++;
			}
			if (drawdownCalc.getCurrentDD() < maxCurrentDdDepth) {
				maxCurrentDdDepth = drawdownCalc.getCurrentDD();
			}
		}
		showAnalysis(lines, ddLengths, drawdownCalc, maxCurrentDdDepth, ddDepths, currentDDLen);
	}

	private static void showAnalysis(List<String> lines, List<Double> ddLengths, DrawdownCalc drawdownCalc, double maxCurrentDdDepth, List<Double> ddDepths, double currentDDLen) {
		if (ddLengths.size() < 10) {
			lines.add(format("Need at least 10 recovered drawdowns, got %d", ddLengths.size()));
			return;
		}
		double maxDd = drawdownCalc.getMaxDD();
		if (maxCurrentDdDepth > maxDd) {
			lines.add(format("Maximum of current drawdown (%.1f %%) is smaller than the biggest (%.1f %%). No statistical analysis needed", maxCurrentDdDepth, maxDd));
		} else {
			double probability = analyze(ddDepths, maxCurrentDdDepth);
			lines.add(format("Maximum of current drawdown depth (%.1f%%) probability is %.2f", maxCurrentDdDepth, probability));
		}
		double maxDdLength = ddLengths.stream().mapToDouble(x -> x).max().getAsDouble();
		if (currentDDLen < maxDdLength) {
			lines.add(format("Current drawdown (%.0f bars) is shorter than the longest (%.0f bars). No statistical analysis needed", currentDDLen, maxDdLength));
		} else {
			double probability = analyze(ddLengths, currentDDLen);
			lines.add(format("Current drawdown length (%.0f bars) probability is %.2f", currentDDLen, probability));
		}
	}

	private static double analyze(List<Double> data, double current) {
		double xm = estimateXm(data);
		double alpha = estimateAlpha(data, xm);
		return calculateProbability(current, xm, alpha);
	}

	private static double estimateXm(List<Double> data) {
		Collections.sort(data);
		int index = (int) (0.9 * data.size());
		return data.get(index);
	}

	private static double estimateAlpha(List<Double> data, double xm) {
		int n = 0;
		double sumLn = 0.0;
		for (Double value : data) {
			if (value >= xm) {
				n++;
				sumLn += Math.log(value / xm);
			}
		}
		return n / sumLn;
	}

	private static double calculateProbability(double x, double xm, double alpha) {
		if (x < xm) {
			return 1.0;
		}
		return Math.pow(xm / x, alpha);
	}

	private RecentDrawdownChecker() {
	}
}

package com.alphatica.alis.trading.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;

public class RecentDrawdownChecker {

	public static void checkDrawdown(List<Double> navHistory, Consumer<String> stringConsumer) {
		if (navHistory.size() < 10) {
			stringConsumer.accept(format("Need at least 10 history points, got %d", navHistory.size()));
		} else {
			doAnalysis(navHistory, stringConsumer);
		}
	}

	private static void doAnalysis(List<Double> navHistory, Consumer<String> stringConsumer) {
		stringConsumer.accept(format("Getting account history from %d data points", navHistory.size()));
		DrawDownCalc drawDownCalc = new DrawDownCalc();
		List<Double> ddLengths = new ArrayList<>();
		List<Double> ddDepths = new ArrayList<>();
		double currentDDLen = 0;
		double maxCurrentDdDepth = 0.0;
		for (Double nav : navHistory) {
			drawDownCalc.updateNav(nav);
			if (drawDownCalc.getCurrentDD() == 0 && currentDDLen > 0) {
				ddLengths.add(currentDDLen);
				currentDDLen = 0;
				ddDepths.add(maxCurrentDdDepth);
				maxCurrentDdDepth = 0;
			} else {
				currentDDLen++;
			}
			if (drawDownCalc.getCurrentDD() < maxCurrentDdDepth) {
				maxCurrentDdDepth = drawDownCalc.getCurrentDD();
			}
		}
		showAnalysis(stringConsumer, ddLengths, drawDownCalc, maxCurrentDdDepth, ddDepths, currentDDLen);
	}

	private static void showAnalysis(Consumer<String> stringConsumer, List<Double> ddLengths, DrawDownCalc drawDownCalc, double maxCurrentDdDepth, List<Double> ddDepths, double currentDDLen) {
		if (ddLengths.size() < 10) {
			stringConsumer.accept(format("Need at least 10 recovered drawdowns, got %d", ddLengths.size()));
			return;
		}
		double maxDd = drawDownCalc.getMaxDD();
		if (maxCurrentDdDepth > maxDd) {
			stringConsumer.accept(format("Maximum of current drawdown (%.1f %%) is smaller than the biggest (%.1f %%). No statistical analysis needed", maxCurrentDdDepth, maxDd));
		} else {
			double probability = analyze(ddDepths, maxCurrentDdDepth);
			stringConsumer.accept(format("Maximum of current drawdown depth (%.1f%%) probability is %.2f", maxCurrentDdDepth, probability));
		}
		double maxDdLength = ddLengths.stream().mapToDouble(x -> x).max().getAsDouble();
		if (currentDDLen < maxDdLength) {
			stringConsumer.accept(format("Current drawdown (%.0f bars) is shorter than the longest (%.0f bars). No statistical analysis needed", currentDDLen, maxDdLength));
		} else {
			double probability = analyze(ddLengths, currentDDLen);
			stringConsumer.accept(format("Current drawdown length (%.0f bars) probability is %.2f", currentDDLen, probability));
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

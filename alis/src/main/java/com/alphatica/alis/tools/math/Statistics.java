package com.alphatica.alis.tools.math;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Statistics {

	private Statistics() {
	}

	public static double zscore(double raw, double mean, double stDev) {
		// https://goodcalculators.com/p-value-calculator/
		// z = 1.65 -> p-value = 0.049
		// z = 2.3 -> p-value = 0.01
		return (raw - mean) / stDev;
	}

	public static double monteCarlo(List<Double> needle, List<Double> haystack) {
		if (needle.size() > haystack.size() / 2) {
			return Double.NaN;
		}
		double sumNeedle = needle.stream().mapToDouble(Double::doubleValue).sum();
		int reached = 0;
		int test = 1_000_000;

		int needleSize = needle.size();
		int haystackSize = haystack.size();
		double[] haystackArray = haystack.stream().mapToDouble(Double::doubleValue).toArray();

		for (int i = 0; i < test; i++) {
			double currentSum = 0;
			boolean[] used = new boolean[haystackSize];
			for (int j = 0; j < needleSize; j++) {
				int index;
				do {
					index = ThreadLocalRandom.current().nextInt(haystackSize);
				} while (used[index]);
				used[index] = true;
				currentSum += haystackArray[index];
			}
			if (currentSum > sumNeedle) {
				reached++;
			}
		}
		return (double) reached / test;
	}

	public static double stDev(List<Double> values, double mean) {
		double sum = 0.0;
		for (Double value : values) {
			sum += Math.pow(value - mean, 2);
		}
		return Math.sqrt(sum / values.size());
	}

	public static double mean(List<Double> values) {
		double sum = 0;
		for (Double value : values) {
			sum += value;
		}
		return sum / values.size();
	}

	public static double median(List<Double> nums) {
		int n = nums.size();
		if (n % 2 == 1) {
			return quickSelect(nums, 0, n - 1, n / 2);
		} else {
			return 0.5 * (quickSelect(nums, 0, n - 1, n / 2 - 1) + quickSelect(nums, 0, n - 1, n / 2));
		}
	}

	private static double quickSelect(List<Double> nums, int left, int right, int k) {
		while (left <= right) {
			int pivotIndex = left + ThreadLocalRandom.current().nextInt(right - left + 1);
			pivotIndex = partition(nums, left, right, pivotIndex);

			if (k == pivotIndex) {
				return nums.get(k);
			} else if (k < pivotIndex) {
				right = pivotIndex - 1;
			} else {
				left = pivotIndex + 1;
			}
		}
		throw new IllegalArgumentException("k is out of bounds");
	}

	private static int partition(List<Double> nums, int left, int right, int pivotIndex) {
		double pivotValue = nums.get(pivotIndex);
		swap(nums, pivotIndex, right);
		int storeIndex = left;

		for (int i = left; i < right; i++) {
			if (nums.get(i) < pivotValue) {
				swap(nums, storeIndex, i);
				storeIndex++;
			}
		}
		swap(nums, right, storeIndex);
		return storeIndex;
	}

	private static void swap(List<Double> nums, int i, int j) {
		double temp = nums.get(i);
		nums.set(i, nums.get(j));
		nums.set(j, temp);
	}
}

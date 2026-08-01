package com.alphatica.alis.tools.math;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Statistics {

	private static final int MONTE_CARLO_TRIALS = 1_000_000;

	private Statistics() {
	}

	public static double zscore(double raw, double mean, double stDev) {
		// https://goodcalculators.com/p-value-calculator/
		// z = 1.65 -> p-value = 0.049
		// z = 2.3 -> p-value = 0.01
		return (raw - mean) / stDev;
	}

	public static double monteCarlo(List<Double> needle, List<Double> haystack) {
		Objects.requireNonNull(needle, "needle must not be null");
		Objects.requireNonNull(haystack, "haystack must not be null");

		if (needle.size() > haystack.size() / 2) {
			return Double.NaN;
		}

		double needleSum = sum(needle);
		int needleSize = needle.size();
		int haystackSize = haystack.size();

		double[] haystackValues = toDoubleArray(haystack);
		int[] indices = createIndices(haystackSize);

		ThreadLocalRandom random = ThreadLocalRandom.current();
		int reached = 0;

		for (int trial = 0; trial < MONTE_CARLO_TRIALS; trial++) {
			double currentSum = 0.0;

			/*
			 * Select values without replacement using a partial Fisher-Yates
			 * shuffle. The indices array can be reused because every permutation
			 * is a valid starting state for the next trial.
			 */
			for (int i = 0; i < needleSize; i++) {
				int selectedIndex = random.nextInt(i, haystackSize);
				swap(indices, i, selectedIndex);
				currentSum += haystackValues[indices[i]];
			}

			if (currentSum > needleSum) {
				reached++;
			}
		}

		return (double) reached / MONTE_CARLO_TRIALS;
	}

	public static double stDev(List<Double> values, double mean) {
		requireNonEmpty(values, "values");

		double squaredDifferenceSum = 0.0;
		for (Double value : values) {
			double difference = requireValue(value) - mean;
			squaredDifferenceSum += difference * difference;
		}

		return Math.sqrt(squaredDifferenceSum / values.size());
	}

	public static double mean(List<Double> values) {
		requireNonEmpty(values, "values");
		return sum(values) / values.size();
	}

	public static double median(List<Double> nums) {
		requireNonEmpty(nums, "nums");
		return medianInPlace(toDoubleArray(nums));
	}

	public static double median(double[] nums) {
		requireNonEmpty(nums, "nums");

		// QuickSelect rearranges elements, so operate on a copy.
		return medianInPlace(Arrays.copyOf(nums, nums.length));
	}

	public static float median(float[] nums) {
		requireNonEmpty(nums, "nums");

		// QuickSelect rearranges elements, so operate on a copy.
		return medianInPlace(Arrays.copyOf(nums, nums.length));
	}

	private static double medianInPlace(double[] nums) {
		int middle = nums.length / 2;

		if ((nums.length & 1) == 1) {
			return quickSelect(nums, 0, nums.length - 1, middle);
		}

		double lower = quickSelect(nums, 0, nums.length - 1, middle - 1);
		double upper = quickSelect(nums, 0, nums.length - 1, middle);

		// Dividing before adding avoids overflow for large finite values.
		return lower / 2.0 + upper / 2.0;
	}

	private static float medianInPlace(float[] nums) {
		int middle = nums.length / 2;

		if ((nums.length & 1) == 1) {
			return quickSelect(nums, 0, nums.length - 1, middle);
		}

		float lower = quickSelect(nums, 0, nums.length - 1, middle - 1);
		float upper = quickSelect(nums, 0, nums.length - 1, middle);

		return lower / 2.0f + upper / 2.0f;
	}

	private static double quickSelect(
			double[] nums,
			int left,
			int right,
			int k) {

		ThreadLocalRandom random = ThreadLocalRandom.current();

		while (left <= right) {
			int pivotIndex = random.nextInt(left, right + 1);
			int partitionIndex = partition(nums, left, right, pivotIndex);

			if (partitionIndex == k) {
				return nums[partitionIndex];
			}

			if (k < partitionIndex) {
				right = partitionIndex - 1;
			} else {
				left = partitionIndex + 1;
			}
		}

		throw new IllegalArgumentException("k is out of bounds: " + k);
	}

	private static float quickSelect(
			float[] nums,
			int left,
			int right,
			int k) {

		ThreadLocalRandom random = ThreadLocalRandom.current();

		while (left <= right) {
			int pivotIndex = random.nextInt(left, right + 1);
			int partitionIndex = partition(nums, left, right, pivotIndex);

			if (partitionIndex == k) {
				return nums[partitionIndex];
			}

			if (k < partitionIndex) {
				right = partitionIndex - 1;
			} else {
				left = partitionIndex + 1;
			}
		}

		throw new IllegalArgumentException("k is out of bounds: " + k);
	}

	private static int partition(
			double[] nums,
			int left,
			int right,
			int pivotIndex) {

		double pivotValue = nums[pivotIndex];
		swap(nums, pivotIndex, right);

		int storeIndex = left;

		for (int i = left; i < right; i++) {
			if (Double.compare(nums[i], pivotValue) < 0) {
				swap(nums, storeIndex, i);
				storeIndex++;
			}
		}

		swap(nums, storeIndex, right);
		return storeIndex;
	}

	private static int partition(
			float[] nums,
			int left,
			int right,
			int pivotIndex) {

		float pivotValue = nums[pivotIndex];
		swap(nums, pivotIndex, right);

		int storeIndex = left;

		for (int i = left; i < right; i++) {
			if (Float.compare(nums[i], pivotValue) < 0) {
				swap(nums, storeIndex, i);
				storeIndex++;
			}
		}

		swap(nums, storeIndex, right);
		return storeIndex;
	}

	private static double sum(List<Double> values) {
		double sum = 0.0;

		for (Double value : values) {
			sum += requireValue(value);
		}

		return sum;
	}

	private static double[] toDoubleArray(List<Double> values) {
		double[] result = new double[values.size()];

		for (int i = 0; i < values.size(); i++) {
			result[i] = requireValue(values.get(i));
		}

		return result;
	}

	private static int[] createIndices(int size) {
		int[] indices = new int[size];

		for (int i = 0; i < size; i++) {
			indices[i] = i;
		}

		return indices;
	}

	private static double requireValue(Double value) {
		return Objects.requireNonNull(value, "values must not contain null");
	}

	private static void requireNonEmpty(List<?> values, String argumentName) {
		Objects.requireNonNull(values, argumentName + " must not be null");

		if (values.isEmpty()) {
			throw new IllegalArgumentException(
					argumentName + " must not be empty");
		}
	}

	private static void requireNonEmpty(double[] values, String argumentName) {
		Objects.requireNonNull(values, argumentName + " must not be null");

		if (values.length == 0) {
			throw new IllegalArgumentException(
					argumentName + " must not be empty");
		}
	}

	private static void requireNonEmpty(float[] values, String argumentName) {
		Objects.requireNonNull(values, argumentName + " must not be null");

		if (values.length == 0) {
			throw new IllegalArgumentException(
					argumentName + " must not be empty");
		}
	}

	private static void swap(double[] values, int first, int second) {
		double temporary = values[first];
		values[first] = values[second];
		values[second] = temporary;
	}

	private static void swap(float[] values, int first, int second) {
		float temporary = values[first];
		values[first] = values[second];
		values[second] = temporary;
	}

	private static void swap(int[] values, int first, int second) {
		int temporary = values[first];
		values[first] = values[second];
		values[second] = temporary;
	}
}
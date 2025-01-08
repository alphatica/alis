package com.alphatica.alis.tools.math;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArraySlice;

public class Regressions {
	private Regressions() {
	}

	public static double calculateLinearRegression(TimeMarketData data, int len, int offset, int at) {
		DoubleArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Double.NaN;
		}

		double sumX = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumX2 = 0;

		for (int i = 0; i < len; i++) {
			double x = i;
			double y = closes.get(i + offset);
			sumX += x;
			sumY += y;
			sumXY += x * y;
			sumX2 += x * x;
		}
		double lenSum = (len * sumX2 - sumX * sumX);
		if (lenSum == 0) {
			return Double.NaN;
		}
		double a = (len * sumXY - sumX * sumY) / lenSum;

		double b = (sumY - a * sumX) / len;
		return a * at + b;
	}

	public static double calculateQuadraticRegression(TimeMarketData data, int len, int offset, int at) {
		DoubleArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Double.NaN;
		}

		double sumX = 0;
		double sumX2 = 0;
		double sumX3 = 0;
		double sumX4 = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumX2Y = 0;

		for (int i = 0; i < len; i++) {
			double x = i; // Using indices as x values
			double x2 = x * x;
			double x3 = x2 * x;
			double x4 = x3 * x;

			sumX += x;
			sumX2 += x2;
			sumX3 += x3;
			sumX4 += x4;
			sumY += closes.get(i + offset);
			sumXY += x * closes.get(i + offset);
			sumX2Y += x2 * closes.get(i + offset);
		}

		// Solve the normal equations for a quadratic regression
		// [ n     Σx     Σx²  ] [ c ]   [ Σy    ]
		// [ Σx    Σx²    Σx³  ] [ b ] = [ Σxy   ]
		// [ Σx²   Σx³    Σx⁴  ] [ a ]   [ Σx²y  ]

		double[][] matrix = {{len, sumX, sumX2}, {sumX, sumX2, sumX3}, {sumX2, sumX3, sumX4}};

		double[] constants = {sumY, sumXY, sumX2Y};

		// Solve the linear system to find coefficients a, b, c
		double[] coefficients = solveLinearSystem(matrix, constants);

		double a = coefficients[2];
		double b = coefficients[1];
		double c = coefficients[0];

		return a * at * at + b * at + c;
	}

	public static double calculateCubicRegression(TimeMarketData data, int len, int offset, int at) {
		DoubleArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Double.NaN;
		}

		// Compute sums for cubic regression
		double sumX = 0;
		double sumX2 = 0;
		double sumX3 = 0;
		double sumX4 = 0;
		double sumX5 = 0;
		double sumX6 = 0;
		double sumY = 0;
		double sumXY = 0;
		double sumX2Y = 0;
		double sumX3Y = 0;

		for (int i = 0; i < len; i++) {
			double xi = i; // Use index as x
			double xi2 = xi * xi;
			double xi3 = xi2 * xi;
			double xi4 = xi3 * xi;
			double xi5 = xi4 * xi;
			double xi6 = xi5 * xi;

			sumX += xi;
			sumX2 += xi2;
			sumX3 += xi3;
			sumX4 += xi4;
			sumX5 += xi5;
			sumX6 += xi6;

			sumY += closes.get(i + offset);
			sumXY += xi * closes.get(i + offset);
			sumX2Y += xi2 * closes.get(i + offset);
			sumX3Y += xi3 * closes.get(i + offset);
		}

		// Solve the normal equations for cubic regression
		// [ Σx⁰  Σx¹   Σx²   Σx³  ] [ c ]   [ Σy     ]
		// [ Σx¹  Σx²   Σx³   Σx⁴  ] [ b ]   [ Σxy    ]
		// [ Σx²  Σx³   Σx⁴   Σx⁵  ] [ a ] = [ Σx²y   ]
		// [ Σx³  Σx⁴   Σx⁵   Σx⁶  ] [ d ]   [ Σx³y   ]

		double[][] matrix = {{len, sumX, sumX2, sumX3}, {sumX, sumX2, sumX3, sumX4}, {sumX2, sumX3, sumX4, sumX5}, {sumX3, sumX4, sumX5, sumX6}};

		double[] constants = {sumY, sumXY, sumX2Y, sumX3Y};

		// Solve the linear system to find coefficients d, a, b, c
		double[] coefficients = solveLinearSystem(matrix, constants);

		double d = coefficients[3];
		double a = coefficients[2];
		double b = coefficients[1];
		double c = coefficients[0];

		return d * at * at + a * at * at + b * at + c;
	}

	// Solve a system of linear equations using Gaussian elimination
	private static double[] solveLinearSystem(double[][] matrix, double[] constants) {
		int n = constants.length;

		for (int i = 0; i < n; i++) {
			// Partial pivoting
			int max = i;
			for (int k = i + 1; k < n; k++) {
				if (Math.abs(matrix[k][i]) > Math.abs(matrix[max][i])) {
					max = k;
				}
			}

			// Swap rows
			double[] temp = matrix[i];
			matrix[i] = matrix[max];
			matrix[max] = temp;

			double t = constants[i];
			constants[i] = constants[max];
			constants[max] = t;

			// Make all rows below this one 0 in the current column
			for (int k = i + 1; k < n; k++) {
				double factor = matrix[k][i] / matrix[i][i];
				constants[k] -= factor * constants[i];
				for (int j = i; j < n; j++) {
					matrix[k][j] -= factor * matrix[i][j];
				}
			}
		}

		// Solve equation Ax=b for an upper triangular matrix A
		double[] solution = new double[n];
		for (int i = n - 1; i >= 0; i--) {
			double sum = 0.0;
			for (int j = i + 1; j < n; j++) {
				sum += matrix[i][j] * solution[j];
			}
			solution[i] = (constants[i] - sum) / matrix[i][i];
		}

		return solution;
	}
}

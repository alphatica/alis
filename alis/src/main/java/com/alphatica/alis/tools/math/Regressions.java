package com.alphatica.alis.tools.math;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.FloatArraySlice;

public class Regressions {
	private Regressions() {
	}

	public static float calculateLinearRegression(TimeMarketData data, int len, int offset, int at) {
		FloatArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Float.NaN;
		}

		float sumX = 0;
		float sumY = 0;
		float sumXY = 0;
		float sumX2 = 0;

		for (int i = 0; i < len; i++) {
			float x = i;
			float y = closes.get(i + offset);
			sumX += x;
			sumY += y;
			sumXY += x * y;
			sumX2 += x * x;
		}
		float lenSum = (len * sumX2 - sumX * sumX);
		if (lenSum == 0) {
			return Float.NaN;
		}
		float a = (len * sumXY - sumX * sumY) / lenSum;

		float b = (sumY - a * sumX) / len;
		return a * at + b;
	}

	public static float calculateQuadraticRegression(TimeMarketData data, int len, int offset, int at) {
		FloatArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Float.NaN;
		}

		float sumX = 0;
		float sumX2 = 0;
		float sumX3 = 0;
		float sumX4 = 0;
		float sumY = 0;
		float sumXY = 0;
		float sumX2Y = 0;

		for (int i = 0; i < len; i++) {
			float x = i; // Using indices as x values
			float x2 = x * x;
			float x3 = x2 * x;
			float x4 = x3 * x;

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

		float[][] matrix = {{len, sumX, sumX2}, {sumX, sumX2, sumX3}, {sumX2, sumX3, sumX4}};

		float[] constants = {sumY, sumXY, sumX2Y};

		// Solve the linear system to find coefficients a, b, c
		float[] coefficients = solveLinearSystem(matrix, constants);

		float a = coefficients[2];
		float b = coefficients[1];
		float c = coefficients[0];

		return a * at * at + b * at + c;
	}

	public static float calculateCubicRegression(TimeMarketData data, int len, int offset, int at) {
		FloatArraySlice closes = data.getLayer(Layer.CLOSE);

		if (closes.size() < len + offset) {
			return Float.NaN;
		}

		// Compute sums for cubic regression
		float sumX = 0;
		float sumX2 = 0;
		float sumX3 = 0;
		float sumX4 = 0;
		float sumX5 = 0;
		float sumX6 = 0;
		float sumY = 0;
		float sumXY = 0;
		float sumX2Y = 0;
		float sumX3Y = 0;

		for (int i = 0; i < len; i++) {
			float xi = i; // Use index as x
			float xi2 = xi * xi;
			float xi3 = xi2 * xi;
			float xi4 = xi3 * xi;
			float xi5 = xi4 * xi;
			float xi6 = xi5 * xi;

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

		float[][] matrix = {{len, sumX, sumX2, sumX3}, {sumX, sumX2, sumX3, sumX4}, {sumX2, sumX3, sumX4, sumX5}, {sumX3, sumX4, sumX5, sumX6}};

		float[] constants = {sumY, sumXY, sumX2Y, sumX3Y};

		// Solve the linear system to find coefficients d, a, b, c
		float[] coefficients = solveLinearSystem(matrix, constants);

		float d = coefficients[3];
		float a = coefficients[2];
		float b = coefficients[1];
		float c = coefficients[0];

		return d * at * at + a * at * at + b * at + c;
	}

	// Solve a system of linear equations using Gaussian elimination
	private static float[] solveLinearSystem(float[][] matrix, float[] constants) {
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
			float[] temp = matrix[i];
			matrix[i] = matrix[max];
			matrix[max] = temp;

			float t = constants[i];
			constants[i] = constants[max];
			constants[max] = t;

			// Make all rows below this one 0 in the current column
			for (int k = i + 1; k < n; k++) {
				float factor = matrix[k][i] / matrix[i][i];
				constants[k] -= factor * constants[i];
				for (int j = i; j < n; j++) {
					matrix[k][j] -= factor * matrix[i][j];
				}
			}
		}

		// Solve equation Ax=b for an upper triangular matrix A
		float[] solution = new float[n];
		for (int i = n - 1; i >= 0; i--) {
			float sum = 0.0f;
			for (int j = i + 1; j < n; j++) {
				sum += matrix[i][j] * solution[j];
			}
			solution[i] = (constants[i] - sum) / matrix[i][i];
		}

		return solution;
	}
}

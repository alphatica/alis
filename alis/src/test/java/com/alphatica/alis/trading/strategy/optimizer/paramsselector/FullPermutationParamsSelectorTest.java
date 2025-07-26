package com.alphatica.alis.trading.strategy.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.ParamSteps;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;
import com.alphatica.alis.trading.optimizer.paramsselector.FullPermutationParamsSelector;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FullPermutationParamsSelectorTest {

	private static final double DELTA = 0.00000000001;

	@SuppressWarnings("java:S5961")
	@Test
	void shouldGiveAllPermutations() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("A", new ParamSteps()); // true, false
		paramsStepsSet.addParamSteps("B", new ParamSteps(3, 1, 5)); // 3, 4, 5
		paramsStepsSet.addParamSteps("C", new ParamSteps(1.0, 0.2, 2.0)); // 1.0, 1.2, 1.4, 1.6, 1.8, 2.0
		FullPermutationParamsSelector selector = new FullPermutationParamsSelector(paramsStepsSet);

		// Expected sequence: C changes slowest, B next, A fastest
		double[] cValues = {1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
		int[] bValues = {3, 4, 5};
		boolean[] aValues = {true, false};
		int iteration = 0;

		for (double c : cValues) {
			for (int b : bValues) {
				for (boolean a : aValues) {
					iteration++;
					Map<String, Object> row = selector.next();
					assertFalse(row.isEmpty(), "Expected non-empty map at iteration " + iteration);
					assertEquals(a, row.get("A"), "A mismatch at iteration " + iteration);
					assertEquals(b, row.get("B"), "B mismatch at iteration " + iteration);
					assertEquals(c, (double) row.get("C"), DELTA, "C mismatch at iteration " + iteration);
				}
			}
		}

		// Verify all 36 combinations (2 × 3 × 6) were checked
		assertEquals(36, iteration, "Expected 36 iterations");
		// Verify selector is finished
		assertTrue(selector.next().isEmpty(), "Expected empty map after all permutations");
	}
}
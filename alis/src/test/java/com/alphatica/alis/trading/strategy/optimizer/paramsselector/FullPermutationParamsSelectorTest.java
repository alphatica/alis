package com.alphatica.alis.trading.strategy.optimizer.paramsselector;

import com.alphatica.alis.trading.strategy.optimizer.ParamSteps;
import com.alphatica.alis.trading.strategy.optimizer.ParamsStepsSet;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FullPermutationParamsSelectorTest {

	private static final double DELTA = 0.00000000001;


	@SuppressWarnings("java:S5961")
	@Test
	void shouldGiveAllPermutations() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("A", new ParamSteps());
		paramsStepsSet.addParamSteps("B", new ParamSteps(3, 1, 5));
		paramsStepsSet.addParamSteps("C", new ParamSteps(1.0, 0.2, 2.0));
		FullPermutationParamsSelector selector = new FullPermutationParamsSelector(paramsStepsSet);
		// Test case 1: {A=true, B=3, C=1.0}
		Map<String, Object> row1 = selector.next();
		assertEquals(true, row1.get("A"));
		assertEquals(3, row1.get("B"));
		assertEquals(1.0, (double) row1.get("C"), DELTA);

		// Test case 2: {A=false, B=3, C=1.0}
		Map<String, Object> row2 = selector.next();
		assertEquals(false, row2.get("A"));
		assertEquals(3, row2.get("B"));
		assertEquals(1.0, (double) row2.get("C"), DELTA);

		// Test case 3: {A=true, B=4, C=1.0}
		Map<String, Object> row3 = selector.next();
		assertEquals(true, row3.get("A"));
		assertEquals(4, row3.get("B"));
		assertEquals(1.0, (double) row3.get("C"), DELTA);

		// Test case 4: {A=false, B=4, C=1.0}
		Map<String, Object> row4 = selector.next();
		assertEquals(false, row4.get("A"));
		assertEquals(4, row4.get("B"));
		assertEquals(1.0, (double) row4.get("C"), DELTA);

		// Test case 5: {A=true, B=5, C=1.0}
		Map<String, Object> row5 = selector.next();
		assertEquals(true, row5.get("A"));
		assertEquals(5, row5.get("B"));
		assertEquals(1.0, (double) row5.get("C"), DELTA);

		// Test case 6: {A=false, B=5, C=1.0}
		Map<String, Object> row6 = selector.next();
		assertEquals(false, row6.get("A"));
		assertEquals(5, row6.get("B"));
		assertEquals(1.0, (double) row6.get("C"), DELTA);

		// Test case 7: {A=true, B=3, C=1.2}
		Map<String, Object> row7 = selector.next();
		assertEquals(true, row7.get("A"));
		assertEquals(3, row7.get("B"));
		assertEquals(1.2, (double) row7.get("C"), DELTA);

		// Test case 8: {A=false, B=3, C=1.2}
		Map<String, Object> row8 = selector.next();
		assertEquals(false, row8.get("A"));
		assertEquals(3, row8.get("B"));
		assertEquals(1.2, (double) row8.get("C"), DELTA);

		// Test case 9: {A=true, B=4, C=1.2}
		Map<String, Object> row9 = selector.next();
		assertEquals(true, row9.get("A"));
		assertEquals(4, row9.get("B"));
		assertEquals(1.2, (double) row9.get("C"), DELTA);

		// Test case 10: {A=false, B=4, C=1.2}
		Map<String, Object> row10 = selector.next();
		assertEquals(false, row10.get("A"));
		assertEquals(4, row10.get("B"));
		assertEquals(1.2, (double) row10.get("C"), DELTA);

		// Test case 11: {A=true, B=5, C=1.2}
		Map<String, Object> row11 = selector.next();
		assertEquals(true, row11.get("A"));
		assertEquals(5, row11.get("B"));
		assertEquals(1.2, (double) row11.get("C"), DELTA);

		// Test case 12: {A=false, B=5, C=1.2}
		Map<String, Object> row12 = selector.next();
		assertEquals(false, row12.get("A"));
		assertEquals(5, row12.get("B"));
		assertEquals(1.2, (double) row12.get("C"), DELTA);

		// Test case 13: {A=true, B=3, C=1.4}
		Map<String, Object> row13 = selector.next();
		assertEquals(true, row13.get("A"));
		assertEquals(3, row13.get("B"));
		assertEquals(1.4, (double) row13.get("C"), DELTA);

		// Test case 14: {A=false, B=3, C=1.4}
		Map<String, Object> row14 = selector.next();
		assertEquals(false, row14.get("A"));
		assertEquals(3, row14.get("B"));
		assertEquals(1.4, (double) row14.get("C"), DELTA);

		// Test case 15: {A=true, B=4, C=1.4}
		Map<String, Object> row15 = selector.next();
		assertEquals(true, row15.get("A"));
		assertEquals(4, row15.get("B"));
		assertEquals(1.4, (double) row15.get("C"), DELTA);

		// Test case 16: {A=false, B=4, C=1.4}
		Map<String, Object> row16 = selector.next();
		assertEquals(false, row16.get("A"));
		assertEquals(4, row16.get("B"));
		assertEquals(1.4, (double) row16.get("C"), DELTA);

		// Test case 17: {A=true, B=5, C=1.4}
		Map<String, Object> row17 = selector.next();
		assertEquals(true, row17.get("A"));
		assertEquals(5, row17.get("B"));
		assertEquals(1.4, (double) row17.get("C"), DELTA);

		// Test case 18: {A=false, B=5, C=1.4}
		Map<String, Object> row18 = selector.next();
		assertEquals(false, row18.get("A"));
		assertEquals(5, row18.get("B"));
		assertEquals(1.4, (double) row18.get("C"), DELTA);

		// Test case 19: {A=true, B=3, C=1.6}
		Map<String, Object> row19 = selector.next();
		assertEquals(true, row19.get("A"));
		assertEquals(3, row19.get("B"));
		assertEquals(1.6, (double) row19.get("C"), DELTA);

		// Test case 20: {A=false, B=3, C=1.6}
		Map<String, Object> row20 = selector.next();
		assertEquals(false, row20.get("A"));
		assertEquals(3, row20.get("B"));
		assertEquals(1.6, (double) row20.get("C"), DELTA);

		// Test case 21: {A=true, B=4, C=1.6}
		Map<String, Object> row21 = selector.next();
		assertEquals(true, row21.get("A"));
		assertEquals(4, row21.get("B"));
		assertEquals(1.6, (double) row21.get("C"), DELTA);

		// Test case 22: {A=false, B=4, C=1.6}
		Map<String, Object> row22 = selector.next();
		assertEquals(false, row22.get("A"));
		assertEquals(4, row22.get("B"));
		assertEquals(1.6, (double) row22.get("C"), DELTA);

		// Test case 23: {A=true, B=5, C=1.6}
		Map<String, Object> row23 = selector.next();
		assertEquals(true, row23.get("A"));
		assertEquals(5, row23.get("B"));
		assertEquals(1.6, (double) row23.get("C"), DELTA);

		// Test case 24: {A=false, B=5, C=1.6}
		Map<String, Object> row24 = selector.next();
		assertEquals(false, row24.get("A"));
		assertEquals(5, row24.get("B"));
		assertEquals(1.6, (double) row24.get("C"), DELTA);

		// Test case 25: {A=true, B=3, C=1.8}
		Map<String, Object> row25 = selector.next();
		assertEquals(true, row25.get("A"));
		assertEquals(3, row25.get("B"));
		assertEquals(1.8, (double) row25.get("C"), DELTA);

		// Test case 26: {A=false, B=3, C=1.8}
		Map<String, Object> row26 = selector.next();
		assertEquals(false, row26.get("A"));
		assertEquals(3, row26.get("B"));
		assertEquals(1.8, (double) row26.get("C"), DELTA);

		// Test case 27: {A=true, B=4, C=1.8}
		Map<String, Object> row27 = selector.next();
		assertEquals(true, row27.get("A"));
		assertEquals(4, row27.get("B"));
		assertEquals(1.8, (double) row27.get("C"), DELTA);

		// Test case 28: {A=false, B=4, C=1.8}
		Map<String, Object> row28 = selector.next();
		assertEquals(false, row28.get("A"));
		assertEquals(4, row28.get("B"));
		assertEquals(1.8, (double) row28.get("C"), DELTA);

		// Test case 29: {A=true, B=5, C=1.8}
		Map<String, Object> row29 = selector.next();
		assertEquals(true, row29.get("A"));
		assertEquals(5, row29.get("B"));
		assertEquals(1.8, (double) row29.get("C"), DELTA);

		// Test case 30: {A=false, B=5, C=1.8}
		Map<String, Object> row30 = selector.next();
		assertEquals(false, row30.get("A"));
		assertEquals(5, row30.get("B"));
		assertEquals(1.8, (double) row30.get("C"), DELTA);

		// Test case 31: {A=true, B=3, C=2.0}
		Map<String, Object> row31 = selector.next();
		assertEquals(true, row31.get("A"));
		assertEquals(3, row31.get("B"));
		assertEquals(2.0, (double) row31.get("C"), DELTA);

		// Finished:
		assertTrue(selector.next().isEmpty());
	}



}
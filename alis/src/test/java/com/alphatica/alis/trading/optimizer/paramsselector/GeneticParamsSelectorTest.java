package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParamSteps;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneticParamsSelectorTest {

	@Test
	void shouldSelectParentsProportionallyToTheirScores() {
		GeneticParamsSelector selector = selector();
		selector.registerScore(score(1, 1));
		selector.registerScore(score(2, 2));
		selector.registerScore(score(3, 3));
		Map<Integer, Integer> selections = new HashMap<>();
		Random random = new Random(1234);
		int attempts = 30_000;

		for (int i = 0; i < attempts; i++) {
			int selected = (int) selector.selectParent(random.nextDouble()).get("value");
			selections.merge(selected, 1, Integer::sum);
		}

		assertEquals(1.0 / 6, selections.get(1) / (double) attempts, 0.01);
		assertEquals(2.0 / 6, selections.get(2) / (double) attempts, 0.01);
		assertEquals(3.0 / 6, selections.get(3) / (double) attempts, 0.01);
	}

	@Test
	void shouldSelectUniformlyWhenAllScoresAreZero() {
		GeneticParamsSelector selector = selector();
		selector.registerScore(score(0, 1));
		selector.registerScore(score(0, 2));

		Set<Object> selected = Set.of(
				selector.selectParent(0.25).get("value"),
				selector.selectParent(0.75).get("value")
		);

		assertEquals(Set.of(1, 2), selected);
	}

	@Test
	void shouldUseShiftedWeightsForNegativeScores() {
		GeneticParamsSelector selector = selector();
		selector.registerScore(score(-10, 1));
		selector.registerScore(score(-5, 2));

		assertEquals(2, selector.selectParent(0).get("value"));
		assertEquals(2, selector.selectParent(0.999).get("value"));
	}

	private static GeneticParamsSelector selector() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(1, 1, 3));
		return new GeneticParamsSelector(paramsStepsSet);
	}

	private static OptimizerScore score(double score, int value) {
		return new OptimizerScore(score, Map.of("value", value));
	}
}

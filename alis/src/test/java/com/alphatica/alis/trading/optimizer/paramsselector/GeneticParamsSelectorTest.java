package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ParamSteps;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticParamsSelectorTest {

	private static final int SAMPLES = 40_000;

	@Test
	void shouldSelectParentsByTournamentOfThreeRanks() {
		GeneticParamsSelector selector = singleParamSelector(new Random(1234));
		selector.registerScore(score(1.50, 1));
		selector.registerScore(score(1.62, 4));
		selector.registerScore(score(1.55, 2));
		selector.registerScore(score(1.60, 3));

		Map<Object, Double> frequencies = parentFrequencies(selector);

		assertEquals(37.0 / 64, frequencies.get(4), 0.01);
		assertEquals(19.0 / 64, frequencies.get(3), 0.01);
		assertEquals(7.0 / 64, frequencies.get(2), 0.01);
		assertEquals(1.0 / 64, frequencies.get(1), 0.005);
	}

	@Test
	void shouldSelectByRankRegardlessOfScoreScaleAndSign() {
		GeneticParamsSelector selector = singleParamSelector(new Random(99));
		selector.registerScore(score(-1_000_000, 1));
		selector.registerScore(score(-10, 2));

		Map<Object, Double> frequencies = parentFrequencies(selector);

		assertEquals(7.0 / 8, frequencies.get(2), 0.01);
		assertEquals(1.0 / 8, frequencies.get(1), 0.01);
	}

	@Test
	void shouldIgnoreScoresThatAreNotFinite() {
		GeneticParamsSelector selector = singleParamSelector(new Random(5));
		selector.registerScore(score(Double.POSITIVE_INFINITY, 2));
		selector.registerScore(score(Double.NaN, 3));
		selector.registerScore(score(Double.NEGATIVE_INFINITY, 2));
		selector.registerScore(score(-7, 1));

		assertEquals(Map.of(1, 1.0), parentFrequencies(selector));
		Set<Object> proposed = new HashSet<>();
		for (int i = 0; i < 2; i++) {
			proposed.add(selector.next().get("value"));
		}
		assertEquals(Set.of(2, 3), proposed);
	}

	@Test
	void shouldProposeOnlyUnseenParamsAlsoBeforeTheyAreScored() {
		GeneticParamsSelector selector = gridSelector(new Random(7));
		Map<String, Object> parent = params(10, 10);
		selector.registerScore(new OptimizerScore(1, parent));
		Set<Map<String, Object>> proposed = new HashSet<>();

		for (int i = 0; i < 120; i++) {
			Map<String, Object> next = selector.next();
			assertNotEquals(parent, next);
			assertTrue(proposed.add(next), "duplicate " + next);
		}
	}

	@Test
	void shouldNotRepeatParamsDuringOptimization() {
		GeneticParamsSelector selector = gridSelector(new Random(11));
		Set<Map<String, Object>> proposed = new HashSet<>();

		for (int i = 0; i < 150; i++) {
			Map<String, Object> next = selector.next();
			assertTrue(proposed.add(next), "duplicate " + next);
			int a = (int) next.get("a");
			int b = (int) next.get("b");
			selector.registerScore(new OptimizerScore(-Math.abs(a - 3) - Math.abs(b - 17), next));
		}
	}

	@Test
	void shouldMutateChildOfIdenticalParentsMostlyByOneStepWithinRange() {
		int oneStep = 0;
		int farther = 0;
		Map<Map<String, Object>, Integer> oneStepChildren = new HashMap<>();
		Random random = new Random(3);
		int trials = 4_000;
		for (int i = 0; i < trials; i++) {
			GeneticParamsSelector selector = gridSelector(random);
			Map<String, Object> parent = params(1, 20);
			selector.registerScore(new OptimizerScore(5, parent));

			Map<String, Object> child = selector.next();

			int a = (int) child.get("a");
			int b = (int) child.get("b");
			assertTrue(a >= 1 && a <= 20 && b >= 1 && b <= 20, "out of range " + child);
			assertNotEquals(parent, child);
			if (a <= 2 && b >= 19) {
				oneStep++;
				oneStepChildren.merge(child, 1, Integer::sum);
			} else {
				farther++;
			}
		}

		assertEquals(Set.of(params(2, 20), params(1, 19), params(2, 19)), oneStepChildren.keySet());
		int difference = oneStepChildren.get(params(2, 20)) - oneStepChildren.get(params(1, 19));
		assertTrue(Math.abs(difference) < 0.05 * trials, "difference " + difference);
		assertTrue(oneStep > 0.8 * trials, "one step " + oneStep);
		assertTrue(farther > 0.05 * trials && farther < 0.25 * trials, "farther " + farther);
	}

	@Test
	void shouldStepToNeighbourValuesInsideRange() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(0.5, 0.25, 1.5));
		Map<Object, Integer> children = new HashMap<>();
		Random random = new Random(21);
		int trials = 2_000;
		for (int i = 0; i < trials; i++) {
			GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, random);
			selector.registerScore(new OptimizerScore(1, Map.of("value", 0.75)));
			children.merge(selector.next().get("value"), 1, Integer::sum);
		}

		assertEquals(Set.of(0.5, 1.0, 1.25, 1.5), children.keySet());
		assertEquals(0.47, children.get(0.5) / (double) trials, 0.06);
		assertEquals(0.47, children.get(1.0) / (double) trials, 0.06);
	}

	@Test
	void shouldCombineParamsOfDifferentParents() {
		Random random = new Random(17);
		int mixed = 0;
		int trials = 2_000;
		for (int i = 0; i < trials; i++) {
			GeneticParamsSelector selector = gridSelector(random);
			selector.registerScore(new OptimizerScore(2, params(1, 1)));
			selector.registerScore(new OptimizerScore(1, params(20, 20)));

			Map<String, Object> child = selector.next();

			int a = (int) child.get("a");
			int b = (int) child.get("b");
			if ((a <= 2 && b >= 19) || (a >= 19 && b <= 2)) {
				mixed++;
			}
		}

		assertTrue(mixed > 0.05 * trials, "mixed " + mixed);
	}

	@Test
	void shouldSometimesProposeRandomImmigrantInsteadOfChild() {
		Random random = new Random(29);
		int far = 0;
		int trials = 4_000;
		for (int i = 0; i < trials; i++) {
			ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
			for (String name : List.of("a", "b", "c", "d", "e", "f", "g", "h")) {
				paramsStepsSet.addParamSteps(name, new ParamSteps(1, 1, 20));
			}
			GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, random);
			Map<String, Object> parent = new HashMap<>();
			for (String name : paramsStepsSet.getParamStepsMap().keySet()) {
				parent.put(name, 1);
			}
			selector.registerScore(new OptimizerScore(1, parent));

			Map<String, Object> child = selector.next();

			long changed = child.values().stream().filter(value -> !value.equals(1)).count();
			if (changed >= 6) {
				far++;
			}
		}

		assertEquals(0.05, far / (double) trials, 0.01);
	}

	@Test
	void shouldStopWhenAllParamsWereProposed() {
		GeneticParamsSelector selector = singleParamSelector(new Random(13));
		selector.registerScore(score(1, 1));
		selector.registerScore(score(2, 3));

		Map<String, Object> last = selector.next();

		assertEquals(Map.of("value", 2), last);
		assertEquals(Map.of(), selector.next());
	}

	@Test
	void shouldProposeRandomParamsBeforeAnyScore() {
		GeneticParamsSelector selector = singleParamSelector(new Random(19));
		Set<Object> proposed = new HashSet<>();
		for (int i = 0; i < 3; i++) {
			proposed.add(selector.next().get("value"));
		}

		assertEquals(Set.of(1, 2, 3), proposed);
		assertEquals(Map.of(), selector.next());
	}

	@Test
	void shouldKeepOnlyBestScoresAsParents() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(1, 1, 66));
		GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, new Random(23));
		for (int value = 1; value <= 65; value++) {
			selector.registerScore(score(value, value));
		}
		selector.registerScore(score(0, 66));

		Set<Object> parents = new HashSet<>();
		for (int i = 0; i < 1_000_000; i++) {
			parents.add(selector.selectParent().get("value"));
		}

		assertFalse(parents.contains(1));
		assertFalse(parents.contains(66));
		assertTrue(parents.contains(65));
	}

	@Test
	void shouldMutateParamOutsideStepsToValueFromSteps() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(0.5, 0.25, 1.5));
		Map<Object, Integer> children = new HashMap<>();
		Random random = new Random(31);
		int trials = 1_000;
		for (int i = 0; i < trials; i++) {
			GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, random);
			selector.registerScore(new OptimizerScore(1, Map.of("value", 0.9)));
			children.merge(selector.next().get("value"), 1, Integer::sum);
		}

		assertEquals(Set.of(0.5, 0.75, 1.0, 1.25, 1.5), children.keySet());
		children.values().forEach(count -> assertEquals(0.2, count / (double) trials, 0.06));
	}

	@Test
	void shouldNotMutateParamWithSingleValue() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("a", new ParamSteps(1, 1, 20));
		paramsStepsSet.addParamSteps("fixed", new ParamSteps(5, 1, 5));
		Random random = new Random(37);
		int stepped = 0;
		int trials = 2_000;
		for (int i = 0; i < trials; i++) {
			GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, random);
			selector.registerScore(new OptimizerScore(1, Map.of("a", 10, "fixed", 5)));

			Map<String, Object> child = selector.next();

			assertEquals(5, child.get("fixed"));
			int a = (int) child.get("a");
			if (a == 9 || a == 11) {
				stepped++;
			}
		}

		assertTrue(stepped > 0.85 * trials, "stepped " + stepped);
	}

	@Test
	void shouldStepFromRepeatedRandomParamsToUnseenNeighbour() {
		GeneticParamsSelector selector = singleParamSelector(() -> 0L);

		assertEquals(Map.of("value", 1), selector.next());
		assertEquals(Map.of("value", 2), selector.next());
	}

	@Test
	void shouldIgnoreRepeatedParamsInPopulation() {
		GeneticParamsSelector selector = singleParamSelector(new Random(41));
		selector.registerScore(score(5, 1));
		selector.registerScore(score(5, 1));
		selector.registerScore(score(1, 2));

		Map<Object, Double> frequencies = parentFrequencies(selector);

		assertEquals(1.0 / 8, frequencies.get(2), 0.01);
	}

	@Test
	void shouldKeepPopulationOfSixtyFourScores() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(1, 1, 64));
		GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, new Random(43));
		for (int value = 1; value <= 64; value++) {
			selector.registerScore(score(value, value));
		}
		selector.registerScore(score(1000, 1));

		assertEquals(0, parentFrequencies(selector).getOrDefault(1, 0.0), 0.001);
	}

	@Test
	void shouldProposeParamsWhenNumberOfPermutationsOverflowsLong() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		for (String name : List.of("a", "b", "c", "d")) {
			paramsStepsSet.addParamSteps(name, new ParamSteps(1, 1, 65_536));
		}
		GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, new Random(47));

		Map<String, Object> next = selector.next();

		assertEquals(Set.of("a", "b", "c", "d"), next.keySet());
	}

	@Test
	void shouldProposeNothingWhenParamHasNoValues() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("a", new ParamSteps(1, 1, 20));
		paramsStepsSet.addParamSteps("empty", new ParamSteps(1, 1, 0));
		GeneticParamsSelector selector = new GeneticParamsSelector(paramsStepsSet, new Random(53));

		assertEquals(Map.of(), selector.next());
	}

	@Test
	void shouldBeCreatedByFactoryForGeneticSelection() {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(1, 1, 3));
		ParamsSelector selector = ParamsSelector.get(ParametersSelection.GENETIC, paramsStepsSet);

		assertTrue(selector instanceof GeneticParamsSelector);
		assertTrue(Set.of(1, 2, 3).contains(selector.next().get("value")));
	}

	private static Map<Object, Double> parentFrequencies(GeneticParamsSelector selector) {
		Map<Object, Integer> counts = new HashMap<>();
		for (int i = 0; i < SAMPLES; i++) {
			counts.merge(selector.selectParent().get("value"), 1, Integer::sum);
		}
		Map<Object, Double> frequencies = new HashMap<>();
		counts.forEach((value, count) -> frequencies.put(value, count / (double) SAMPLES));
		return frequencies;
	}

	private static GeneticParamsSelector singleParamSelector(RandomGenerator random) {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("value", new ParamSteps(1, 1, 3));
		return new GeneticParamsSelector(paramsStepsSet, random);
	}

	private static GeneticParamsSelector gridSelector(RandomGenerator random) {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		paramsStepsSet.addParamSteps("a", new ParamSteps(1, 1, 20));
		paramsStepsSet.addParamSteps("b", new ParamSteps(1, 1, 20));
		return new GeneticParamsSelector(paramsStepsSet, random);
	}

	private static Map<String, Object> params(int a, int b) {
		return Map.of("a", a, "b", b);
	}

	private static OptimizerScore score(double score, int value) {
		return new OptimizerScore(score, Map.of("value", value));
	}
}

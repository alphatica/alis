package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParamSteps;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

public class GeneticParamsSelector extends ParamsSelector {
	private static final int MAX_SCORES = 64;
	private static final int TOURNAMENT_SIZE = 3;
	private static final double IMMIGRANT_PROBABILITY = 0.05;
	private static final double RANDOM_MUTATION_PROBABILITY = 0.1;
	private static final int MAX_ATTEMPTS = 10;

	private final List<OptimizerScore> scores = new ArrayList<>();
	private final Set<Map<String, Object>> seen = new HashSet<>();
	private final List<String> mutableNames;
	private final long permutations;
	private final RandomGenerator random;

	public GeneticParamsSelector(ParamsStepsSet paramsStepsSet) {
		this(paramsStepsSet, new Random());
	}

	GeneticParamsSelector(ParamsStepsSet paramsStepsSet, RandomGenerator random) {
		super(paramsStepsSet);
		this.random = random;
		this.mutableNames = paramsStepsSet.getParamStepsMap().entrySet().stream()
				.filter(entry -> entry.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.toList();
		this.permutations = permutations(paramsStepsSet);
	}

	@Override
	public Map<String, Object> next() {
		synchronized (scores) {
			if (seen.size() >= permutations) {
				return Map.of();
			}
			Map<String, Object> next;
			if (scores.isEmpty() || random.nextDouble() < IMMIGRANT_PROBABILITY) {
				next = randomUnseen();
			} else {
				next = childUnseen();
			}
			seen.add(new HashMap<>(next));
			return next;
		}
	}

	@Override
	public void registerScore(OptimizerScore newScore) {
		if (!Double.isFinite(newScore.score())) {
			return;
		}
		synchronized (scores) {
			seen.add(new HashMap<>(newScore.params()));
			if (alreadyPresent(newScore.params())) {
				return;
			}
			scores.add(newScore);
			scores.sort(Comparator.reverseOrder());
			if (scores.size() > MAX_SCORES) {
				scores.removeLast();
			}
		}
	}

	Map<String, Object> selectParent() {
		synchronized (scores) {
			int best = random.nextInt(scores.size());
			for (int i = 1; i < TOURNAMENT_SIZE; i++) {
				best = Math.min(best, random.nextInt(scores.size()));
			}
			return scores.get(best).params();
		}
	}

	private Map<String, Object> childUnseen() {
		Map<String, Object> child = crossover(selectParent(), selectParent());
		mutate(child);
		stepUntilUnseen(child);
		if (seen.contains(child)) {
			return randomUnseen();
		}
		return child;
	}

	private void stepUntilUnseen(Map<String, Object> params) {
		for (int i = 0; i < MAX_ATTEMPTS && seen.contains(params); i++) {
			stepMutation(params, mutableNames.get(random.nextInt(mutableNames.size())));
		}
	}

	private Map<String, Object> crossover(Map<String, Object> parent1, Map<String, Object> parent2) {
		Map<String, Object> child = new HashMap<>();
		for (Map.Entry<String, Object> entry : parent1.entrySet()) {
			if (random.nextBoolean()) {
				child.put(entry.getKey(), entry.getValue());
			} else {
				child.put(entry.getKey(), parent2.get(entry.getKey()));
			}
		}
		return child;
	}

	private void mutate(Map<String, Object> child) {
		for (String name : mutableNames) {
			if (random.nextInt(mutableNames.size()) == 0) {
				if (random.nextDouble() < RANDOM_MUTATION_PROBABILITY) {
					child.put(name, randomValue(steps(name)));
				} else {
					stepMutation(child, name);
				}
			}
		}
	}

	private void stepMutation(Map<String, Object> child, String name) {
		Object[] values = steps(name).values();
		int index = Arrays.asList(values).indexOf(child.get(name));
		if (index < 0) {
			child.put(name, randomValue(steps(name)));
			return;
		}
		int next = random.nextBoolean() ? index + 1 : index - 1;
		if (next < 0 || next >= values.length) {
			next = 2 * index - next;
		}
		child.put(name, values[next]);
	}

	private Map<String, Object> randomUnseen() {
		Map<String, Object> candidate = randomParams();
		for (int i = 1; i < MAX_ATTEMPTS && seen.contains(candidate); i++) {
			candidate = randomParams();
		}
		stepUntilUnseen(candidate);
		return candidate;
	}

	private Map<String, Object> randomParams() {
		Map<String, Object> params = new HashMap<>();
		paramsStepsSet.getParamStepsMap().forEach((name, steps) -> params.put(name, randomValue(steps)));
		return params;
	}

	private Object randomValue(ParamSteps steps) {
		return steps.values()[random.nextInt(steps.size())];
	}

	private ParamSteps steps(String name) {
		return paramsStepsSet.getParamStepsMap().get(name);
	}

	private boolean alreadyPresent(Map<String, Object> newParams) {
		return scores.stream().anyMatch(s -> s.params().equals(newParams));
	}

	private static long permutations(ParamsStepsSet paramsStepsSet) {
		long result = 1;
		for (ParamSteps steps : paramsStepsSet.getParamStepsMap().values()) {
			if (steps.size() != 0 && result > Long.MAX_VALUE / steps.size()) {
				return Long.MAX_VALUE;
			}
			result *= steps.size();
		}
		return result;
	}
}

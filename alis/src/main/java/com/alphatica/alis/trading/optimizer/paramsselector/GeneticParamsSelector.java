package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneticParamsSelector extends ParamsSelector {
	private static final int MAX_SCORES = 64;
	private static final int MAX_SCORES_WITHOUT_IMPROVEMENT = 16;

	private final AtomicInteger noImprovementCounter = new AtomicInteger(0);
	private final List<OptimizerScore> scores = new ArrayList<>();
	private final RandomParamsSelector randomParamsSelector;

	private double scoreOffset = 0;
	private double totalSelectionWeight = 0;

	public GeneticParamsSelector(ParamsStepsSet paramsStepsSet) {
		super(paramsStepsSet);
		randomParamsSelector = new RandomParamsSelector(paramsStepsSet);
	}

	@Override
	public Map<String, Object> next() {
		synchronized (scores) {
			if (noImprovementCounter.get() == MAX_SCORES_WITHOUT_IMPROVEMENT) {
				noImprovementCounter.set(0);
				return randomParamsSelector.next();
			}
			if (scores.isEmpty()) {
				return randomParamsSelector.next();
			}
			Map<String, Object> parent1 = selectParent();
			Map<String, Object> parent2 = selectParent();

			Map<String, Object> next = new HashMap<>();
			for (Map.Entry<String, Object> entry : parent1.entrySet()) {
				if (ThreadLocalRandom.current().nextBoolean()) {
					next.put(entry.getKey(), entry.getValue());
				} else {
					next.put(entry.getKey(), parent2.get(entry.getKey()));
				}
			}
			if (alreadyPresent(next)) {
				return randomParamsSelector.next();
			} else {
				return next;
			}
		}
	}

	@Override
	public void registerScore(OptimizerScore newScore) {
		synchronized (scores) {
			if (scores.size() == MAX_SCORES && scores.getLast().score() > newScore.score()) {
				noImprovementCounter.incrementAndGet();
			}
			if (alreadyPresent(newScore.params())) {
				return;
			}
			scores.add(newScore);
			Collections.sort(scores);
			Collections.reverse(scores);

			if (scores.size() > MAX_SCORES) {
				scores.removeLast();
			}
			updateSelectionWeights();
		}
	}

	private void updateSelectionWeights() {
		double minimumScore = scores.stream().mapToDouble(OptimizerScore::score).min().orElse(0);
		scoreOffset = Math.max(0, -minimumScore);
		totalSelectionWeight = scores.stream().mapToDouble(this::selectionWeight).sum();
	}

	private boolean alreadyPresent(Map<String, Object> newParams) {
		return scores.stream().anyMatch(s -> s.params().equals(newParams));
	}

	private Map<String, Object> selectParent() {
		return selectParent(ThreadLocalRandom.current().nextDouble());
	}

	Map<String, Object> selectParent(double randomFraction) {
		synchronized (scores) {
			if (scores.isEmpty()) {
				throw new IllegalStateException("selectParent couldn't find parent");
			}
			if (totalSelectionWeight == 0) {
				int index = Math.min((int) (randomFraction * scores.size()), scores.size() - 1);
				return scores.get(index).params();
			}

			double selectionPoint = randomFraction * totalSelectionWeight;
			double weightSoFar = 0.0;
			for (OptimizerScore score : scores) {
				weightSoFar += selectionWeight(score);
				if (selectionPoint < weightSoFar) {
					return score.params();
				}
			}
		}
		throw new IllegalStateException("selectParent couldn't find parent");
	}

	private double selectionWeight(OptimizerScore score) {
		return score.score() + scoreOffset;
	}
}

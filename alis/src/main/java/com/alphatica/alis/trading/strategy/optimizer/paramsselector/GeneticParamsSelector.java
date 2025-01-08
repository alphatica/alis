package com.alphatica.alis.trading.strategy.optimizer.paramsselector;

import com.alphatica.alis.trading.strategy.optimizer.OptimizerScore;
import com.alphatica.alis.trading.strategy.optimizer.ParamsStepsSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneticParamsSelector extends ParamsSelector {
	private static final int MAX_SCORES = 64;
	private static final int MAX_SCORES_WITHOUT_IMPROVEMENT = 16;

	private final AtomicInteger noImprovementCounter = new AtomicInteger(0);
	private final List<OptimizerScore> scores = new ArrayList<>();
	private double totalScore = 0;

	public GeneticParamsSelector(ParamsStepsSet paramsStepsSet) {
		super(paramsStepsSet);
	}

	@Override
	public Map<String, Object> next() {
		synchronized (scores) {
			if (noImprovementCounter.get() == MAX_SCORES_WITHOUT_IMPROVEMENT) {
				noImprovementCounter.set(0);
				return Collections.emptyMap();
			}
			if (scores.isEmpty()) {
				return Collections.emptyMap();
			}
			if (totalScore < 1) {
				return Collections.emptyMap();
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
				return Collections.emptyMap();
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
			totalScore = scores.stream().map(OptimizerScore::score).reduce(Double::sum).get();
		}
	}

	private boolean alreadyPresent(Map<String, Object> newParams) {
		return scores.stream().anyMatch(s -> s.params().equals(newParams));
	}

	private Map<String, Object> selectParent() {
		double scoreSoFar = 0.0;

		synchronized (this) {
			for (OptimizerScore score : scores) {
				scoreSoFar += score.score();
				if (ThreadLocalRandom.current().nextDouble(totalScore) < scoreSoFar) {
					return score.params();
				}
			}
		}
		throw new IllegalStateException("selectParent couldn't find parent");
	}
}

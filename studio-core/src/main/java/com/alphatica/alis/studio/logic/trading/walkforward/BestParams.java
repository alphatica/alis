package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.trading.optimizer.OptimizerScore;

import java.util.List;
import java.util.Map;

/** Highest score so far; of equal scores, the first by Java code. */
final class BestParams {
	private OptimizerScore score;
	private List<String> code;

	void offer(OptimizerScore candidate) {
		List<String> candidateCode = candidate.formatParamsAsJavaCode();
		if (score == null || candidate.score() > score.score()
				|| (candidate.score() == score.score() && String.join("\n", candidateCode).compareTo(String.join("\n", code)) < 0)) {
			score = candidate;
			code = candidateCode;
		}
	}

	boolean isEmpty() {
		return score == null;
	}

	double score() {
		return score.score();
	}

	Map<String, Object> params() {
		return score.params();
	}

	List<String> code() {
		return code;
	}
}

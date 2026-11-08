package com.alphatica.alis.trading.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record OptimizerScore(double score, Map<String, Object> params, ResultStats stats) implements Comparable<OptimizerScore> {

	public OptimizerScore(double score, Map<String, Object> params) {
		this(score, params, null);
	}

	@Override
	public int compareTo(OptimizerScore o) {
		return Double.compare(score, o.score);
	}

	public List<String> formatParamsAsJavaCode() {
		List<String> content = new ArrayList<>();
		for(Map.Entry<String, Object> param: params.entrySet()) {
			content.add(formatParamAsJavaCode(param));
		}
		return content;
	}

	private String formatParamAsJavaCode(Map.Entry<String, Object> param) {
		return param.getKey() + " = " + clearDoubleInaccuracies(param.getValue()) + "; ";
	}

	private String clearDoubleInaccuracies(Object value) {
		String raw = value.toString();
		int threeZerosIndex = raw.indexOf("000");
		if (threeZerosIndex < 0) {
			return raw;
		}
		int dotIndex = raw.indexOf("\\.");
		if (threeZerosIndex > dotIndex) {
			return raw.substring(0, threeZerosIndex);
		} else {
			return raw;
		}
	}
}

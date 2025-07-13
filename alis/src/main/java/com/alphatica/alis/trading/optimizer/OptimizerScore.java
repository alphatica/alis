package com.alphatica.alis.trading.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record OptimizerScore(double score, Map<String, Object> params) implements Comparable<OptimizerScore> {
	@Override
	public int compareTo(OptimizerScore o) {
		return Double.compare(score, o.score);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OptimizerScore that = (OptimizerScore) o;
		return Double.compare(score, that.score) == 0 && Objects.equals(params, that.params);
	}

	@Override
	public int hashCode() {
		return Objects.hash(score, params);
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

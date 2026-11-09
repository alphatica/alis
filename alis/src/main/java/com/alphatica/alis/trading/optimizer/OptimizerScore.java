package com.alphatica.alis.trading.optimizer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record OptimizerScore(double score, Map<String, Object> params, ResultStats stats) implements Comparable<OptimizerScore> {
	private static final MathContext PARAM_PRECISION = new MathContext(12);

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
		return param.getKey() + " = " + formatParamValue(param.getValue()) + "; ";
	}

	public static String formatParamValue(Object value) {
		if (value instanceof Double number && Double.isFinite(number)) {
			String text = new BigDecimal(number).round(PARAM_PRECISION).stripTrailingZeros().toPlainString();
			return text.contains(".") ? text : text + ".0";
		}
		return String.valueOf(value);
	}
}

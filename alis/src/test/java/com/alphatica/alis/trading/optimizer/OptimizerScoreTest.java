package com.alphatica.alis.trading.optimizer;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptimizerScoreTest {

	@Test
	void shouldKeepWholeNumbersWithZeros() {
		assertEquals("1000", OptimizerScore.formatParamValue(1000));
		assertEquals("10005", OptimizerScore.formatParamValue(10005));
		assertEquals("-2000", OptimizerScore.formatParamValue(-2000));
	}

	@Test
	void shouldClearInaccuraciesOfDoubles() {
		assertEquals("0.68", OptimizerScore.formatParamValue(0.5 + 0.02 * 9));
		assertEquals("0.3", OptimizerScore.formatParamValue(0.29999999999999993));
		assertEquals("0.7", OptimizerScore.formatParamValue(0.7000000000000001));
	}

	@Test
	void shouldKeepSignificantDigitsOfDoubles() {
		assertEquals("0.1000000005", OptimizerScore.formatParamValue(0.1000000005));
		assertEquals("1000.5", OptimizerScore.formatParamValue(1000.5));
		assertEquals("0.00025", OptimizerScore.formatParamValue(0.00025));
		assertEquals("-1.25", OptimizerScore.formatParamValue(-1.25));
	}

	@Test
	void shouldShowDoublesAsDoubles() {
		assertEquals("1000.0", OptimizerScore.formatParamValue(1000.0));
		assertEquals("0.0", OptimizerScore.formatParamValue(0.0));
	}

	@Test
	void shouldShowOtherValuesAsText() {
		assertEquals("true", OptimizerScore.formatParamValue(true));
		assertEquals("NaN", OptimizerScore.formatParamValue(Double.NaN));
		assertEquals("Infinity", OptimizerScore.formatParamValue(Double.POSITIVE_INFINITY));
	}

	@Test
	void shouldFormatParamsAsJavaCodeInTheirOrder() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("period", 1000);
		params.put("level", 0.5 + 0.02 * 9);
		params.put("enabled", false);

		assertEquals(List.of("period = 1000; ", "level = 0.68; ", "enabled = false; "),
				new OptimizerScore(1, params).formatParamsAsJavaCode());
	}
}

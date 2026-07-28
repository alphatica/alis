package com.alphatica.alis.charting;

final class PaneValueTransformer {

	private final Scale scale;

	PaneValueTransformer(Scale scale) {
		this.scale = scale;
	}

	double transform(double value) {
		if (scale == Scale.ARITHMETIC) {
			return value;
		}
		return Math.copySign(Math.log1p(Math.abs(value)), value);
	}

	double inverse(double value) {
		if (scale == Scale.ARITHMETIC) {
			return value;
		}
		return Math.copySign(Math.expm1(Math.abs(value)), value);
	}
}

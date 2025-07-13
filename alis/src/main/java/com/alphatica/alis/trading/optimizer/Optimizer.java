package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.optimizer.params.DoubleParam;
import com.alphatica.alis.trading.optimizer.params.IntParam;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class Optimizer {

	public static long computeAllPermutations(Optimizable optimizable) {
		return buildParamsStepsSet(optimizable.getClass().getDeclaredFields()).computePermutations();
	}

	protected static ParamsStepsSet buildParamsStepsSet(Field[] fields) {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		for (Field field : fields) {
			if (field.isAnnotationPresent(BoolParam.class)) {
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps());
			}
			if (field.isAnnotationPresent(IntParam.class)) {
				IntParam p = field.getAnnotation(IntParam.class);
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps(p.start(), p.step(), p.end()));
			}
			if (field.isAnnotationPresent(DoubleParam.class)) {
				DoubleParam p = field.getAnnotation(DoubleParam.class);
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps(p.start(), p.step(), p.end()));
			}
		}
		return paramsStepsSet;
	}

	@SuppressWarnings("java:S3011")
	protected void copyParameters(Map<String, Object> params, Optimizable optimizable) throws IllegalAccessException {
		Field[] fields = optimizable.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (isParameterField(field)) {
				field.setAccessible(true);
				field.set(optimizable, params.get(field.getName()));
			}
		}
		optimizable.paramsChanged();
	}

	private static boolean isParameterField(Field field) {
		return field.isAnnotationPresent(BoolParam.class) || field.isAnnotationPresent(IntParam.class) || field.isAnnotationPresent(DoubleParam.class);
	}

}

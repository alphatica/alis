package com.alphatica.alis.trading.strategy.params;

import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.optimizer.OptimizerException;

import java.lang.reflect.Field;

import static java.lang.String.format;

public class Validator {

	private Validator() {
	}

	public static void validate(Strategy strategy) throws OptimizerException {
		Field[] fields = strategy.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(BoolParam.class)) {
				validateBool(field);
			}
			if (field.isAnnotationPresent(IntParam.class)) {
				validateInt(field);
			}
			if (field.isAnnotationPresent(DoubleParam.class)) {
				validateDouble(field);
			}
		}
	}

	private static void validateDouble(Field field) throws OptimizerException {
		checkValidClass(field, double.class, Double.class);
		checkDoubleRange(field);
	}

	private static void validateInt(Field field) throws OptimizerException {
		checkValidClass(field, int.class, Integer.class);
		checkIntRange(field);
	}

	private static void validateBool(Field field) throws OptimizerException {
		checkValidClass(field, boolean.class, Boolean.class);
	}

	private static void checkValidClass(Field field, Class<?> classA, Class<?> classB) throws OptimizerException {
		if (!field.getType().equals(classA) && !field.getType().equals(classB)) {
			throw new OptimizerException(format("Field `%s` has a type `%s` but optimized as `%s`", field.getName(), field.getType()
																														  .getName(),
					classA.getName()));
		}
	}

	private static void checkDoubleRange(Field field) throws OptimizerException {
		if (field.isAnnotationPresent(DoubleParam.class)) {
			DoubleParam doubleParam = field.getAnnotation(DoubleParam.class);
			if (doubleParam.start() + doubleParam.step() > doubleParam.end()) {
				throw new OptimizerException(format("Invalid start/step/stop for parameter field `%s`", field.getName()));
			}
		}
	}

	private static void checkIntRange(Field field) throws OptimizerException {
		if (field.isAnnotationPresent(IntParam.class)) {
			IntParam intParam = field.getAnnotation(IntParam.class);
			if (intParam.start() + intParam.step() > intParam.end()) {
				throw new OptimizerException(format("Invalid start/step/stop for parameter field `%s`", field.getName()));
			}
		}
	}
}

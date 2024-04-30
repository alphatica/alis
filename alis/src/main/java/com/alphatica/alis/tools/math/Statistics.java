package com.alphatica.alis.tools.math;

import java.util.List;

public class Statistics {

    private Statistics() {
    }

    public static double zscore(double raw, double mean, double stDev) {
        // https://goodcalculators.com/p-value-calculator/
        // z = 1.65 -> p-value = 0.049
        // z = 2.3 -> p-value = 0.01
        return (raw - mean) / stDev;
    }

    public static double stDev(List<Double> values, double mean) {
        double sum = 0.0;
        for (Double value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }

    public static double mean(List<Double> values) {
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public static double median(List<Double> values) {
        int middle = values.size() / 2;
        if (values.size() % 2 == 0) {
            return (values.get(middle) + values.get(middle + 1)) / 2;
        } else {
            return values.get(middle);
        }
    }
}

package com.alphatica.alis.data.riskfreerate;

import com.alphatica.alis.data.time.Time;

/**
 * Days with one risk-free rate.
 *
 * @param from first day
 * @param to   last day, inclusive
 * @param rate annual rate as a fraction, e.g. 0.0375 for 3.75 %
 */
public record RatePeriod(Time from, Time to, double rate) {
}

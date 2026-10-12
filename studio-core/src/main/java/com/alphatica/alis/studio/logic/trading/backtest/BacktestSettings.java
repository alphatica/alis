package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.function.Supplier;

/**
 * Settings of a strategy run, shared by the backtest and the optimization.
 *
 * @param strategyFactory a new strategy with default parameters on every call
 */
public record BacktestSettings(Time timeStart, Time timeEnd, double commissionRate, double initialCapital,
                               Supplier<Strategy> strategyFactory) {
}

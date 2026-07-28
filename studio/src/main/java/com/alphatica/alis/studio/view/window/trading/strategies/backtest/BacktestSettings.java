package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.strategy.Strategy;

record BacktestSettings(Time timeStart, Time timeEnd, double commissionRate, double initialCapital, Strategy strategy) {
}

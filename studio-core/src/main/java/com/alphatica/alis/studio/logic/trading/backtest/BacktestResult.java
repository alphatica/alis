package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

import java.util.List;

/**
 * @param times sessions of the run
 * @param nav   account value at the close of each session; the last one after closing all positions
 */
public record BacktestResult(Account account, int missedTrades, List<Time> times, List<Double> nav) {
}

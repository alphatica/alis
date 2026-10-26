package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.trading.account.Account;

/**
 * @param account the account after the run, with the NAV of every session in its history (the last one after
 *                closing all positions)
 */
public record BacktestResult(Account account, int missedTrades) {
}

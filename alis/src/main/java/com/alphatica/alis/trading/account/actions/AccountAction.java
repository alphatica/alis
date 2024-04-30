package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;

public record AccountAction(Time time, AccountActionType actionType) {
}

package com.alphatica.alis.examples.donchian;

public record DonchianResult(int buyPeriod, int sellPeriod, boolean sortByHighestGrowth, double finalNav) {
}

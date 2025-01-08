package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketName;

public record SellInfo(MarketName marketName, int quantity) {
}

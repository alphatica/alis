package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class Account {
    private final Map<MarketName, Position> positions = new HashMap<>();
    private double cash;

    public Account(double cash) {
        this.cash = cash;
    }

    public Optional<Position> getPosition(MarketName market) {
        return ofNullable(positions.get(market));
    }

    public void addTrade(MarketName market, double count) {
        Position position = positions.get(market);
        if (position == null) {
            positions.put(market, new Position(count));
        } else {
            double newQuantity = position.updateQuantity(count);
            if (newQuantity == 0.0) {
                positions.remove(market);
            }
        }
    }

    public void addCash(double cash) {
        this.cash += cash;
    }

    public double getCash() {
        return cash;
    }

    public double getNAV() {
        double nav = cash;
        for (Position position : positions.values()) {
            nav += position.getLastPrice() * position.getSize();
        }
        return nav;
    }

    public void updateLastKnown(TimeMarketDataSet data) {
        positions.forEach((market, position) -> {
            TimeMarketData marketData = data.get(market);
            if (marketData != null) {
                double close = marketData.getData(Layer.CLOSE, 0);
                position.updateLastPrice(close);
            }
        });

    }
}

package com.alphatica.alis.trading.signalcheck;


import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;

public class OpenTrade {
    private final TradeSignal signal;
    private final float position;
    private final float openPrice;

    private float lastKnowPrice;
    private int bars;

    public OpenTrade(TradeSignal signal, float openPrice, float position) {
        this.signal = signal;
        this.openPrice = openPrice;
        this.position = position;
        this.bars = 0;
    }

    public TradeSignal getSignal() {
        return signal;
    }

    public float getPosition() {
        return position;
    }

    public float getLastKnowPrice() {
        return lastKnowPrice;
    }

    public void updateLastKnownPrice(float price) {
        lastKnowPrice = price;
    }

    public void incrementBars() {
        bars++;
    }

    public int getBars() {
        return bars;
    }

    public float getOpenPrice() {
        return openPrice;
    }
}

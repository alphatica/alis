package com.alphatica.alis.trading.signalcheck;


import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;

import static com.alphatica.alis.trading.signalcheck.TradeStatus.OPEN;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_OPEN;

public class OpenTrade {
    private final TradeSignal signal;
    private final float positionSize;

    private float openPrice;
    private float lastKnowPrice;
    private TradeStatus tradeStatus;
    private int bars;

    public OpenTrade(TradeSignal signal, float positionSize) {
        this.signal = signal;
        this.openPrice = Float.NaN;
        this.tradeStatus = PENDING_OPEN;
        this.bars = 0;
		this.positionSize = positionSize;
	}

    public void setOpenPrice(float price) {
        this.openPrice = price;
        tradeStatus = OPEN;
    }

    public TradeStatus getTradeStatus() {
        return tradeStatus;
    }

    public TradeSignal getSignal() {
        return signal;
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

    public void setStatus(TradeStatus newStatus) {
        this.tradeStatus = newStatus;
    }

    public float getPositionSize() {
        return positionSize;
    }
}

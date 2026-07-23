package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;

import java.util.Objects;

import static com.alphatica.alis.trading.signalcheck.TradeStatus.OPEN;
import static com.alphatica.alis.trading.signalcheck.TradeStatus.PENDING_OPEN;

public class OpenTrade {
	private final SignalGenerator signalGenerator;
	private final BuySignal buySignal;
	private final Time signalTime;

	private Time openTime;
	private int openEventIndex = -1;
	private float effectiveOpenPrice = Float.NaN;
	private float lastKnownPrice = Float.NaN;
	private Time lastKnownTime;
	private TradeStatus tradeStatus = PENDING_OPEN;
	private int bars;

	public OpenTrade(SignalGenerator signalGenerator, BuySignal buySignal, Time signalTime) {
		this.signalGenerator = Objects.requireNonNull(signalGenerator, "signalGenerator");
		this.buySignal = Objects.requireNonNull(buySignal, "buySignal");
		this.signalTime = Objects.requireNonNull(signalTime, "signalTime");
	}

	public void open(float price, Time time, int eventIndex) {
		if (!Float.isFinite(price) || price <= 0.0f) {
			throw new IllegalArgumentException("price must be finite and positive");
		}
		if (eventIndex < 0) {
			throw new IllegalArgumentException("eventIndex must not be negative");
		}
		this.effectiveOpenPrice = price;
		this.openTime = Objects.requireNonNull(time, "time");
		this.openEventIndex = eventIndex;
		this.tradeStatus = OPEN;
	}

	public void updateLastKnownPrice(float price, Time time) {
		lastKnownPrice = price;
		lastKnownTime = Objects.requireNonNull(time, "time");
	}

	public void incrementBars() {
		bars++;
	}

	public void setStatus(TradeStatus newStatus) {
		tradeStatus = Objects.requireNonNull(newStatus, "newStatus");
	}

	public SignalGenerator getSignalGenerator() {
		return signalGenerator;
	}

	public BuySignal getBuySignal() {
		return buySignal;
	}

	public double getRequestedAllocation() {
		return buySignal.requestedAllocation();
	}

	public double getPriority() {
		return buySignal.priority();
	}

	public Time getSignalTime() {
		return signalTime;
	}

	public Time getOpenTime() {
		return openTime;
	}

	public int getOpenEventIndex() {
		return openEventIndex;
	}

	public float getEffectiveOpenPrice() {
		return effectiveOpenPrice;
	}

	public float getLastKnownPrice() {
		return lastKnownPrice;
	}

	public Time getLastKnownTime() {
		return lastKnownTime;
	}

	public TradeStatus getTradeStatus() {
		return tradeStatus;
	}

	public int getBars() {
		return bars;
	}
}

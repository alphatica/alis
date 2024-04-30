package com.alphatica.alis.condition.changecheck;

import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.time.Time;

import java.util.List;
import java.util.function.Predicate;

public class ChangeCheck {
	private Condition condition;
	private Time from;
	private Time to;
	private Predicate<Market> marketFilter;
	private int windowLength;
	private List<Double> movesLevels;

	private ChangeCheck() {
		from = new Time(0);
		to = new Time(Integer.MAX_VALUE);
		marketFilter = market -> true;
		windowLength = 20;
		movesLevels = List.of();
	}

	public static ChangeCheck condition(Condition condition) {
		ChangeCheck changeCheck = new ChangeCheck();
		changeCheck.condition = condition;
		return changeCheck;
	}

	public ChangeCheck from(Time from) {
		this.from = from;
		return this;
	}

	public ChangeCheck to(Time to) {
		this.to = to;
		return this;
	}

	public ChangeCheck withHigherThanMoves(List<Double> higherThanMoves) {
		this.movesLevels = higherThanMoves;
		return this;
	}

	public ChangeCheck marketFilter(Predicate<Market> filter) {
		this.marketFilter = filter;
		return this;
	}

	public ChangeCheck windowLength(int windowLength) {
		this.windowLength = windowLength;
		return this;
	}

	public Condition getCondition() {
		return condition;
	}

	public Time getFrom() {
		return from;
	}

	public Time getTo() {
		return to;
	}

	public int getWindowLength() {
		return windowLength;
	}

	public List<Double> getMovesLevels() {
		return movesLevels;
	}

	public Predicate<Market> getMarketFilter() {
		return marketFilter;
	}
}

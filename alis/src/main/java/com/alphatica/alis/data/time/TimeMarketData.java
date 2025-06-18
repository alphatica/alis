package com.alphatica.alis.data.time;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.tools.data.FloatArraySlice;

import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.OPEN;

public class TimeMarketData {

	private final MarketName marketName;
	private final MarketType marketType;
	private final Time time;
	private final List<FloatArraySlice> data;

	public TimeMarketData(MarketName marketName, MarketType marketType, Time time, List<FloatArraySlice> data) {
		this.marketName = marketName;
		this.marketType = marketType;
		this.time = time;
		this.data = data;
	}

	public MarketName getMarketName() {
		return marketName;
	}

	public MarketType getMarketType() {
		return marketType;
	}

	public Time getTime() {
		return time;
	}

	public float getData(Layer layer, int index) {
		return data.get(layer.getIndex()).get(index);
	}

	public float getAveragePrice(int index) {
		return (getData(OPEN, index) + getData(HIGH, index) + getData(LOW, index) + getData(CLOSE, index)) / 4;
	}

	public FloatArraySlice getLayer(Layer layer) {
		return data.get(layer.getIndex());
	}
}

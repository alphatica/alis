package com.alphatica.alis.data.time;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.FloatArraySlice;

import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.OPEN;

public class TimeMarketData {

	private final MarketName marketName;
	private final MarketType marketType;
	private final Time time;
	private final List<FloatArraySlice> slices;
	private final float[][] columns;
	private final int row;

	public TimeMarketData(MarketName marketName, MarketType marketType, Time time, List<FloatArraySlice> data) {
		this(marketName, marketType, time, data, null, 0);
	}

	public TimeMarketData(MarketName marketName, MarketType marketType, Time time, float[][] columns, int row) {
		this(marketName, marketType, time, null, columns, row);
	}

	private TimeMarketData(MarketName marketName, MarketType marketType, Time time, List<FloatArraySlice> slices,
						   float[][] columns, int row) {
		this.marketName = marketName;
		this.marketType = marketType;
		this.time = time;
		this.slices = slices;
		this.columns = columns;
		this.row = row;
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
		if (slices != null) {
			return slices.get(layer.getIndex()).get(index);
		}
		float[] column = columns[layer.getIndex()];
		int size = column.length - row;
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		return column[row + index];
	}

	public float getAveragePrice(int index) {
		return (getData(OPEN, index) + getData(HIGH, index) + getData(LOW, index) + getData(CLOSE, index)) / 4;
	}

	public FloatArraySlice getLayer(Layer layer) {
		if (slices != null) {
			return slices.get(layer.getIndex());
		}
		return new FloatArraySlice(columns[layer.getIndex()], row);
	}
}

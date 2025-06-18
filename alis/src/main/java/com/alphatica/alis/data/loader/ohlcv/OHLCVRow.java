package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.OPEN;

public class OHLCVRow {

	private Time time;
	private List<Float> data;

	public static OHLCVRow fromParts(String date, String open, String high, String low, String close, String volume) {
		OHLCVRow quote = new OHLCVRow();
		quote.data = new ArrayList<>();
		quote.time = new Time(Integer.parseInt(date.replace(" ", "").replace("-", "")));
		quote.data.add(Float.parseFloat(open));
		quote.data.add(Float.parseFloat(high));
		quote.data.add(Float.parseFloat(low));
		quote.data.add(Float.parseFloat(close));
		quote.data.add(Float.parseFloat(volume) * (quote.get(OPEN) + quote.get(HIGH) + quote.get(LOW) + quote.get(CLOSE)) / 4.0f);
		quote.data.add(-1.0f);
		quote.data.add(-1.0f);
		quote.data.add(-1.0f);
		return quote;
	}

	public float get(Layer layer) {
		return data.get(layer.getIndex());
	}

	public Time getTime() {
		return time;
	}

	public void parseAndSet(String text, Layer layer) {
		this.data.set(layer.getIndex(), Float.parseFloat(text));
	}

}

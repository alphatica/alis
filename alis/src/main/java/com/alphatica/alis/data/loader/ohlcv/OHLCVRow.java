package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.Time;

public class OHLCVRow {

	private Time time;
	private float[] data;

	public static OHLCVRow fromParts(String date, String open, String high, String low, String close, String volume) {
		OHLCVRow quote = new OHLCVRow();
		quote.time = new Time(Integer.parseInt(date.replace(" ", "").replace("-", "")));
		float parsedVolume = Float.parseFloat(volume);
		float parsedOpen = Float.parseFloat(open);
		float parsedHigh = Float.parseFloat(high);
		float parsedLow = Float.parseFloat(low);
		float parsedClose = Float.parseFloat(close);
		float turnover = parsedVolume * (parsedOpen + parsedHigh + parsedLow + parsedClose) / 4.0f;
		quote.data = new float[]{parsedOpen, parsedHigh, parsedLow, parsedClose, parsedVolume, turnover, Float.NaN, Float.NaN,
				Float.NaN};
		return quote;
	}

	public float get(Layer layer) {
		return data[layer.getIndex()];
	}

	public Time getTime() {
		return time;
	}

	public void parseAndSet(String text, Layer layer) {
		this.data[layer.getIndex()] = Float.parseFloat(text);
	}

}

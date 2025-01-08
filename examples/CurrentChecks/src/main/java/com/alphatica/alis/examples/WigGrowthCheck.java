package com.alphatica.alis.examples;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.oscilators.WilliamsR;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class WigGrowthCheck {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNewPL(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		wig(stooqData);
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void wig(MarketData stooqData) {
		Market wig = stooqData.getMarket(new MarketName("wig"));
		TimeMarketData data = wig.getAtOrNext(new Time(1998_09_08));
		WilliamsR williamsR = new WilliamsR(250);
		final int length = 250;
		int remainingSessions = 0;
		double firstValue = 0;
		boolean mute = false;
		while (data != null) {
			if (data.getLayer(CLOSE).size() <= 250) {
				data = wig.getAtOrNext(data.getTime().next());
				continue;
			}
			double now = data.getData(CLOSE, 0);
			double five = data.getData(CLOSE, 5);
			if (now / five > 1.05 && williamsR.calculate(data) > 0.8 && !mute && remainingSessions <= 0) {
				System.out.print(data.getTime() + " ");
				firstValue = now;
				remainingSessions = length;
				mute = true;
			}
			if (williamsR.calculate(data) < 0.8) {
				mute = false;
			}
			remainingSessions--;
			if (remainingSessions == 0) {
				double change = (now / firstValue - 1) * 100;
				System.out.printf("Change: %.0f%%%n", change);
			}
			data = wig.getAtOrNext(data.getTime().next());
		}
	}
}

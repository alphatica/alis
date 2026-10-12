package com.alphatica.alis.studio.state;

import com.alphatica.alis.data.market.MarketData;

public class AppState {

	private static volatile MarketData marketData;
	private static volatile String dataStatus = "Data not loaded";

	private AppState() {
	}

	public static MarketData getMarketData() {
		return marketData;
	}

	public static void setMarketData(MarketData marketData) {
		AppState.marketData = marketData;
	}

	public static String getDataStatus() {
		return dataStatus;
	}

	public static void setDataStatus(String dataStatus) {
		AppState.dataStatus = dataStatus;
	}

}

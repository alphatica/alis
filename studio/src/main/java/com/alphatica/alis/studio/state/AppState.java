package com.alphatica.alis.studio.state;

import com.alphatica.alis.data.market.MarketData;

public class AppState {

	private static MarketData marketData;
	private static String dataStatus = "Data not loaded";

	private AppState() {
	}

	public static MarketData getMarketData() {
		return marketData;
	}

	public static void setMarketData(MarketData marketData) {
		AppState.marketData = marketData;
		ChangeListeners.publish(StateChange.DATA_LOADED);
	}

	public static String getDataStatus() {
		return dataStatus;
	}

	public static void setDataStatus(String dataStatus) {
		AppState.dataStatus = dataStatus;
		ChangeListeners.publish(StateChange.DATA_STATUS_CHANGED);
	}

}

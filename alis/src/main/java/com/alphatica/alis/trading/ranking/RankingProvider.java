package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.MarketScore;

import java.util.List;

public interface RankingProvider {
	List<MarketScore> getRanking(TimeMarketDataSet data);
}

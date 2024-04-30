package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.market.MarketFilter.ALL;

public class TimeMarketDataSet {
    private final Map<MarketName, TimeMarketData> set;

    public TimeMarketDataSet(Map<MarketName, TimeMarketData> set) {
        this.set = set;
    }

    public static TimeMarketDataSet build(Time time, MarketData marketData) {
        List<Market> markets = marketData.listMarkets(ALL);
        Map<MarketName, TimeMarketData> result = HashMap.newHashMap(markets.size());
        for (Market market : markets) {
            market.getAt(time).ifPresent(timeData -> result.put(market.getName(), timeData));
        }
        return new TimeMarketDataSet(result);
    }

    public TimeMarketData get(MarketName name) {
        return set.get(name);
    }

    public List<TimeMarketData> listMarkets() {
        return set.values().stream().toList();
    }
}

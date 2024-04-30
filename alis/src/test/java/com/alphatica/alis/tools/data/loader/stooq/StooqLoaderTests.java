package com.alphatica.alis.tools.data.loader.stooq;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArrayRange;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StooqLoaderTests {

    private static Optional<Market> loadStooqMarket(MarketName name) throws ExecutionException, InterruptedException {
        String path = Path.of("src", "test", "resources").toAbsolutePath().toString();
        MarketData data = StooqLoader.load(path);
        return data.getMarket(name);
    }

    @Test
    void shouldLoadStock() throws ExecutionException, InterruptedException {
        Optional<Market> maybeMarket = loadStooqMarket(new MarketName("ind"));
        assertTrue(maybeMarket.isPresent());
        Market market = maybeMarket.get();
        assertEquals(MarketType.INDICE, market.getType());
        Time timeFirst = new Time(20100104);
        Optional<TimeMarketData> firstMaybeTimeMarketData = market.getAtOrNext(timeFirst);
        assertTrue(firstMaybeTimeMarketData.isPresent());
        TimeMarketData firstTimeMarketData = firstMaybeTimeMarketData.get();
        assertEquals(timeFirst, firstTimeMarketData.getTime());
        double open = firstTimeMarketData.getData(Layer.OPEN, 0);
        assertEquals(1.1, open, 0.001);

        Time middleTime = new Time(20100110);

        Optional<TimeMarketData> beforeMiddleMaybeTimeMarketData = market.getAtOrPrevious(middleTime);
        assertTrue(beforeMiddleMaybeTimeMarketData.isPresent());
        TimeMarketData beforeMiddleTimeMarketData = beforeMiddleMaybeTimeMarketData.get();
        assertTrue(beforeMiddleTimeMarketData.getTime().isBefore(middleTime));
        assertEquals(5.2, beforeMiddleTimeMarketData.getData(Layer.CLOSE, 0), 0.001);

        Optional<TimeMarketData> afterMiddleMaybeTimeMarketData = market.getAtOrNext(middleTime);
        assertTrue(afterMiddleMaybeTimeMarketData.isPresent());
        TimeMarketData afterMiddleTimeMarketData = afterMiddleMaybeTimeMarketData.get();
        assertTrue(afterMiddleTimeMarketData.getTime().isAfter(middleTime));
        assertEquals(6.5, afterMiddleTimeMarketData.getData(Layer.HIGH, 0), 0.001);
    }

    @Test
    void shouldLoadPartOfHistory() throws ExecutionException, InterruptedException {
        Optional<Market> maybeMarket = loadStooqMarket(new MarketName("ind"));
        assertTrue(maybeMarket.isPresent());
        Market market = maybeMarket.get();
        Optional<TimeMarketData> maybeMarketData = market.getAtOrNext(new Time(20100111));
        assertTrue(maybeMarketData.isPresent());
        TimeMarketData marketData = maybeMarketData.get();
        DoubleArrayRange closes = marketData.getLayer(Layer.CLOSE);
        assertEquals(6.2, closes.get(0), 0.001);
        assertEquals(5.2, closes.get(1), 0.001);
        assertEquals(4.2, closes.get(2), 0.001);
        assertEquals(3.2, closes.get(3), 0.001);
        assertEquals(2.2, closes.get(4), 0.001);
        assertEquals(1.2, closes.get(5), 0.001);
    }

}

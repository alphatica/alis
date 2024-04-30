package com.alphatica.alis.tools.data;

import com.alphatica.alis.data.time.TimeMarketData;

import static com.alphatica.alis.data.layer.Layer.TURNOVER;

public class Liquidity {

    private Liquidity() {
    }

    public static boolean isLiquid(TimeMarketData timeMarketData, double minTurnover, int bars) {
        double turnoverSoFar = 0.0;
        DoubleArrayRange turnoverData = timeMarketData.getLayer(TURNOVER);
        for (int i = 0; i < bars && i < turnoverData.size(); i++) {
            turnoverSoFar += turnoverData.get(i);
            if (turnoverSoFar >= minTurnover) {
                return true;
            }
        }
        return false;
    }
}

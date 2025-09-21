package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

public class SmaMinMaxCheck {

	static StrategyExecutor check(MarketData stooqData, int smaLength, int minMaxLength, Time start, Time end) throws AccountActionException {
		MinMax minMax = new MinMax(minMaxLength);
		Sma sma = new Sma(smaLength);
		StrategyExecutor strategyExecutor = new StrategyExecutor().withTimeRange(start, end);
		SmaWithMinMax smaWithMinMax = new SmaWithMinMax(minMax, sma);
		Account account = strategyExecutor.execute(stooqData, smaWithMinMax);
		System.out.printf("%.0f %d %d%n", account.getNAV(), smaLength, minMaxLength);
		return strategyExecutor;
	}

    private SmaMinMaxCheck() {
    }
}

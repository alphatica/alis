package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.signalcheck.tradesignal.BuyAthSellSmaTradeSignal;
import com.alphatica.alis.trading.signalcheck.tradesignal.DonchianChannelSignal;
import com.alphatica.alis.trading.signalcheck.tradesignal.SmaCrossTradeSignal;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class Main {

    private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

    @SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
    public static void main(String[] args) throws ExecutionException, InterruptedException, AccountActionException {
        MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
        var signalExecutor = new SignalExecutor(SmaCrossTradeSignal::new,
                new Time(20150101), new Time(20260101), stooqData, STOCKS, 0.01f, false);
        signalExecutor.execute();
    }
}

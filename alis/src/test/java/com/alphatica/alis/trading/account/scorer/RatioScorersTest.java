package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestMarket;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RatioScorersTest {
	private static final double DELTA = 1e-12;
	private static final MarketName MARKET = new MarketName("market");
	// Mon-Wed and Mon, with the built-in rate of 3.75 % since 2026-03-05
	private static final List<Time> TIMES = List.of(new Time(20261019), new Time(20261020), new Time(20261021),
			new Time(20261026));
	private static final float[] PRICES = {55, 44, 66, 60.5f};
	/** 500 cash and 10 units: NAV 1050, 940, 1160, 1105 */
	private static final double[] RETURNS = {0.05, 940.0 / 1050 - 1, 1160.0 / 940 - 1, 1105.0 / 1160 - 1};
	private static final List<RatePeriod> PERIODS = List.of(
			new RatePeriod(TIMES.getFirst(), TIMES.getLast(), 0.0375));

	/** Quoted also on the Friday before the first session, so the first session has a benchmark return */
	private static final TestMarket BENCHMARK = TestMarket.withCloses(new MarketName("wig"),
			new int[]{20261016, 20261019, 20261020, 20261021, 20261026}, new float[]{100, 102, 101, 104, 103});
	private static final double[] BENCHMARK_RETURNS = {102.0 / 100 - 1, 101.0 / 102 - 1, 104.0 / 101 - 1, 103.0 / 104 - 1};

	private static RiskFreeRates rates;

	@TempDir
	static Path directory;

	@BeforeAll
	static void loadRates() throws IOException {
		rates = RiskFreeRates.load(directory);
	}

	@Test
	void shouldScoreSharpeWithRiskFreeRateOfCountry() throws AccountActionException {
		double score = new SharpeRatio(rates, Country.PL).score(tradedAccount(), Map.of());

		assertEquals(ReturnRatios.sharpe(RETURNS, TIMES, PERIODS), score, DELTA);
		assertNotEquals(ReturnRatios.sharpe(RETURNS, TIMES, zeroRate()), score, 1e-3);
	}

	@Test
	void shouldScoreSortinoWithRiskFreeRateOfCountry() throws AccountActionException {
		double score = new SortinoRatio(rates, Country.PL).score(tradedAccount(), Map.of());

		assertEquals(ReturnRatios.sortino(RETURNS, TIMES, PERIODS), score, DELTA);
		assertNotEquals(ReturnRatios.sortino(RETURNS, TIMES, zeroRate()), score, 1e-3);
	}

	@Test
	void shouldScoreZeroWhenRiskFreeRatioIsUndefined() {
		Account noSessions = new Account(1000);
		Account oneSession = new Account(1000);
		oneSession.updateLastKnown(prices(50, TIMES.getFirst()));

		assertEquals(0, new SharpeRatio(rates, Country.PL).score(noSessions, Map.of()));
		assertEquals(0, new SharpeRatio(rates, Country.PL).score(oneSession, Map.of()));
		assertEquals(0, new SortinoRatio(rates, Country.PL).score(noSessions, Map.of()));
		assertEquals(0, new SortinoRatio(rates, Country.PL).score(oneSession, Map.of()));
	}

	@Test
	void shouldScoreJensensAlphaAgainstBenchmarkAtSessions() throws AccountActionException {
		double score = new JensensAlpha(rates, Country.PL, BENCHMARK).score(tradedAccount(), Map.of());

		assertEquals(ReturnRatios.versusBenchmark(RETURNS, BENCHMARK_RETURNS, TIMES, PERIODS).jensenAlphaPercent(),
				score, DELTA);
		assertNotEquals(0, score);
		assertNotEquals(ReturnRatios.versusBenchmark(RETURNS, BENCHMARK_RETURNS, TIMES, zeroRate()).jensenAlphaPercent(),
				score, 1e-3);
	}

	@Test
	void shouldMatchBenchmarkBySessionDateWhenAccountStartsLater() throws AccountActionException {
		// As with "Fuzzy start time": the same benchmark, a later first session
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(TIMES.get(1), 10, 50), 0);
		for (int i = 1; i < TIMES.size(); i++) {
			account.updateLastKnown(prices(PRICES[i], TIMES.get(i)));
		}
		List<Time> times = TIMES.subList(1, TIMES.size());
		double[] returns = {940.0 / 1000 - 1, 1160.0 / 940 - 1, 1105.0 / 1160 - 1};
		double[] benchmarkReturns = Arrays.copyOfRange(BENCHMARK_RETURNS, 1, BENCHMARK_RETURNS.length);
		List<RatePeriod> periods = List.of(new RatePeriod(times.getFirst(), times.getLast(), 0.0375));

		double score = new JensensAlpha(rates, Country.PL, BENCHMARK).score(account, Map.of());

		assertEquals(ReturnRatios.versusBenchmark(returns, benchmarkReturns, times, periods).jensenAlphaPercent(),
				score, DELTA);
	}

	@Test
	void shouldScoreZeroWhenJensensAlphaIsUndefined() throws AccountActionException {
		Account oneSession = new Account(1000);
		oneSession.updateLastKnown(prices(50, TIMES.getFirst()));
		TestMarket notQuotedAtSessions = TestMarket.withCloses(new MarketName("wig"), new int[]{20261001, 20261002},
				new float[]{100, 101});

		assertEquals(0, new JensensAlpha(rates, Country.PL, BENCHMARK).score(new Account(1000), Map.of()));
		assertEquals(0, new JensensAlpha(rates, Country.PL, BENCHMARK).score(oneSession, Map.of()));
		assertEquals(0, new JensensAlpha(rates, Country.PL, notQuotedAtSessions).score(tradedAccount(), Map.of()));
	}

	@Test
	void shouldScoreCagrOfSessionReturns() throws AccountActionException {
		double score = new Cagr().score(tradedAccount(), Map.of());

		// Growth 1105 / 1000 in 4 sessions, a year of 252 sessions
		assertEquals((Math.pow(1.105, 252.0 / 4) - 1) * 100, score, 1e-6);
		assertEquals(0, new Cagr().score(new Account(1000), Map.of()));
	}

	@Test
	void shouldScoreCalmarFromSessionReturnsAndMaxDrawdown() throws AccountActionException {
		Account account = tradedAccount();

		double score = new CalmarRatio().score(account, Map.of());

		assertEquals(940.0 / 1050 * 100 - 100, account.getMaxDD(), 1e-9);
		assertEquals(ReturnRatios.calmar(RETURNS, account.getMaxDD()), score, DELTA);
	}

	@Test
	void shouldScoreTotalReturnOverMaxDownsideDrawdown() throws AccountActionException {
		Account account = tradedAccount();

		double score = new ReturnToMaxDownsideDD().score(account, Map.of());

		// Downside drawdown from closed equity 1000 to NAV 940; total return 1105 / 1000
		assertEquals(-6, account.getMaxDownsideDD(), 1e-9);
		assertEquals(10.5 / 6, score, 1e-9);
	}

	@Test
	void shouldScoreZeroWithoutDrawdown() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(TIMES.getFirst(), 10, 50), 0);
		account.updateLastKnown(prices(55, TIMES.get(0)));
		account.updateLastKnown(prices(60, TIMES.get(1)));

		assertEquals(0, new CalmarRatio().score(account, Map.of()));
		assertEquals(0, new ReturnToMaxDownsideDD().score(account, Map.of()));
		assertEquals(0, new ReturnToMaxDownsideDD().score(new Account(1000), Map.of()));
	}

	private static Account tradedAccount() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(TIMES.getFirst(), 10, 50), 0);
		for (int i = 0; i < TIMES.size(); i++) {
			account.updateLastKnown(prices(PRICES[i], TIMES.get(i)));
		}
		return account;
	}

	private static List<RatePeriod> zeroRate() {
		return List.of(new RatePeriod(TIMES.getFirst(), TIMES.getLast(), 0));
	}

	private static TimeMarketDataSet prices(float price, Time time) {
		FloatArraySlice prices = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice volume = new FloatArraySlice(new float[]{1}, 0);
		TimeMarketData data = new TimeMarketData(MARKET, MarketType.STOCK, time,
				List.of(prices, prices, prices, prices, volume, volume));
		return new TimeMarketDataSet(Map.of(MARKET, data), time);
	}
}

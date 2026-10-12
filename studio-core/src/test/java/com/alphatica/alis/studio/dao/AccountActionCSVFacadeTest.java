package com.alphatica.alis.studio.dao;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static com.alphatica.alis.trading.order.Direction.SELL;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountActionCSVFacadeTest {

	@Test
	void shouldIgnoreCommentsAndEmptyLines() throws IOException {
		String content = """
				# account history
				20231030,Deposit,150000.00

				20250723,Trade,gig,Sell,2.05,4025,0.00 # delisting
				""";

		List<AccountAction> actions = AccountActionCSVFacade.readActions(new StringReader(content));

		assertEquals(List.of(
				new AccountAction(new Time(20231030), new Deposit(150000.0)),
				new AccountAction(new Time(20250723), new Trade(new MarketName("gig"), SELL, 2.05, 4025, 0.0))), actions);
	}
}

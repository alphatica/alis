package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.studio.dao.AccountActionCSVFacade;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.web.live.LiveHub;
import com.alphatica.alis.trading.account.Quantities;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.AccountActionType;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.account.actions.Withdrawal;
import com.alphatica.alis.trading.order.Direction;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Portfolio: account statistics, actions and trade history. Computed once per portfolio or market data change.
 * Adding a trade, a deposit / withdrawal or importing a file tells open GUI tabs to reload the portfolio.
 */
final class PortfolioApi {
	private static final String PATH = "/api/portfolio";

	private final LiveHub hub;
	/** Changed by every save, so the cached portfolio is computed again. */
	private final AtomicLong revision = new AtomicLong();
	private final StateCache<String> json = new StateCache<>(() -> Json.write(PortfolioView.load()),
			() -> new PortfolioState(AppState.getMarketData(), revision.get()));

	PortfolioApi(LiveHub hub) {
		this.hub = hub;
	}

	void register(RoutesConfig routes) {
		routes.get(PATH, ctx -> ctx.contentType("application/json").result(json.get()));
		routes.post(PATH + "/trade", ctx -> save(ctx, trade(ctx.bodyAsClass(TradeRequest.class))));
		routes.post(PATH + "/cash", ctx -> save(ctx, cash(ctx.bodyAsClass(CashRequest.class))));
		routes.post(PATH + "/import", this::importFile);
	}

	/**
	 * Body: a text file with one action per line, as in the account file ('#' starts a comment).
	 * The actions are added to the existing ones, sorted by time.
	 */
	private void importFile(Context ctx) throws Exception {
		List<AccountAction> actions;
		try (Reader reader = new InputStreamReader(ctx.bodyInputStream(), StandardCharsets.UTF_8)) {
			actions = AccountActionCSVFacade.readActions(reader);
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw new ValidationException(e.getMessage());
		}
		if (actions.isEmpty()) {
			throw new ValidationException("No account actions in the file");
		}
		try {
			AccountProvider.saveActions(actions);
		} catch (AccountActionException e) {
			throw new ValidationException(e.getMessage());
		}
		changed();
		ctx.json(Map.of("imported", actions.size()));
	}

	private void save(Context ctx, AccountAction action) throws IOException {
		try {
			AccountProvider.saveAction(action);
		} catch (AccountActionException e) {
			throw new ValidationException(e.getMessage());
		}
		changed();
		ctx.status(HttpStatus.NO_CONTENT);
	}

	private void changed() {
		revision.incrementAndGet();
		hub.broadcast("portfolio-changed", null);
	}

	private static AccountAction trade(TradeRequest request) {
		Direction direction = direction(request.direction());
		return new AccountAction(Inputs.time(request.time(), "time"), new Trade(
				new MarketName(Inputs.text(request.market(), "market")),
				direction,
				Inputs.number(request.price(), "price"),
				quantity(request.quantity()),
				Inputs.number(request.commission(), "commission")));
	}

	private static AccountAction cash(CashRequest request) {
		double amount = Inputs.number(request.amount(), "amount");
		AccountActionType type = switch (request.type() == null ? "" : request.type()) {
			case "deposit" -> new Deposit(amount);
			case "withdrawal" -> new Withdrawal(amount);
			default -> throw new ValidationException("Unknown cash operation: " + request.type());
		};
		return new AccountAction(Inputs.time(request.time(), "time"), type);
	}

	private static Direction direction(String value) {
		return switch (value == null ? "" : value) {
			case "buy" -> Direction.BUY;
			case "sell" -> Direction.SELL;
			default -> throw new ValidationException("Unknown order: " + value);
		};
	}

	private static double quantity(String value) {
		double quantity = Inputs.number(value, "quantity");
		if (!Quantities.isValid(quantity)) {
			throw new ValidationException("The quantity must be at least " + Quantities.format(Quantities.MIN_QUANTITY)
					+ " with at most " + Quantities.DECIMAL_PLACES + " decimal places");
		}
		return quantity;
	}

	private record PortfolioState(MarketData marketData, long revision) {
	}

	/**
	 * @param direction "buy" or "sell"
	 */
	record TradeRequest(String time, String market, String direction, String price, String quantity, String commission) {
	}

	/**
	 * @param type "deposit" or "withdrawal"
	 */
	record CashRequest(String time, String type, String amount) {
	}
}

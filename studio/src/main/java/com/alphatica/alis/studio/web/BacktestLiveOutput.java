package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.web.jobs.JobChannel;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.BarExecutedConsumer;

import java.util.List;

/**
 * Sends the orders and the newly closed trades of every session of a backtest, and the progress.
 */
final class BacktestLiveOutput implements BarExecutedConsumer {
	private final JobChannel channel;
	private final int totalBars;
	private int bars;
	private int sentTrades;

	BacktestLiveOutput(JobChannel channel, int totalBars) {
		this.channel = channel;
		this.totalBars = totalBars;
	}

	@Override
	public void execute(Time time, Account account, List<Order> orders) {
		for (Order order : orders) {
			channel.append("orders", new Object[]{time.time(), order.market().name(), order.direction().toString(),
					order.formatSize(), order.priority()});
		}
		sendNewTrades(account);
		bars++;
		channel.replace("progress", new Progress(time.toString(), Math.min(bars, totalBars), totalBars));
	}

	void sendNewTrades(Account account) {
		List<PositionPricesRecord> records = account.getClosedPricesRecords();
		for (; sentTrades < records.size(); sentTrades++) {
			PositionPricesRecord r = records.get(sentTrades);
			channel.append("trades", new Object[]{r.marketName().toString(), r.entryTime().time(),
					r.exitTime() == null ? null : r.exitTime().time(), r.entry(), r.exit(), r.quantity(),
					r.getProfitPercent(), r.getProfitCash(), r.getEntryEfficiency(), r.getExitEfficiency()});
		}
	}

	/**
	 * @param bar number of sessions done, out of {@code bars}
	 */
	record Progress(String time, int bar, int bars) {
	}
}

package com.alphatica.alis.studio.logic.data;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.studio.logic.table.Column;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.alphatica.alis.data.market.MarketFilters.ALL;

public final class MarketsSummary {

	public static final List<Column> COLUMNS = List.of(Column.text("Name"), Column.text("Type"), Column.date("First"),
			Column.date("Last"), Column.number("Price", 2));

	private MarketsSummary() {
	}

	public static List<Object[]> rows(MarketData marketData) {
		List<Object[]> rows = new ArrayList<>();
		for (Market market : marketData.listMarkets(ALL)) {
			TimeMarketData first = market.getAtOrNext(new Time(0));
			if (first == null) {
				continue;
			}
			TimeMarketData last = market.getAtOrPrevious(new Time(Integer.MAX_VALUE));
			rows.add(new Object[]{market.getName().toString(), market.getType().toString(), first.getTime().time(),
					last.getTime().time(), last.getData(Layer.CLOSE, 0)});
		}
		rows.sort(Comparator.comparing(row -> (String) row[0]));
		return rows;
	}
}

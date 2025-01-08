module com.alphatica.alis {
	requires java.desktop;
	requires org.apache.commons.io;

	exports com.alphatica.alis.data;
	exports com.alphatica.alis.data.layer;
	exports com.alphatica.alis.data.loader.stooq;
	exports com.alphatica.alis.data.market;
	exports com.alphatica.alis.data.time;
	exports com.alphatica.alis.trading.account.actions;
	exports com.alphatica.alis.trading.account;
	exports com.alphatica.alis.tools.java;
	exports com.alphatica.alis.trading.strategy;
	exports com.alphatica.alis.condition;
	exports com.alphatica.alis.tools.data;
	exports com.alphatica.alis.trading.order;
	exports com.alphatica.alis.indicators;
	exports com.alphatica.alis.tools.math;
	exports com.alphatica.alis.trading.strategy.params;
}
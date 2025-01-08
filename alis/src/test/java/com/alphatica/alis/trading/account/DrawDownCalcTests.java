package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrawDownCalcTests {

	@Test
	void shouldCalcSimple() {
		DrawDownCalc dc = new DrawDownCalc();
		dc.updateNav(100);
		dc.updateNav(70);
		dc.updateNav(80);
		assertEquals(-20.0, dc.getCurrentDD(), 0.01);
		assertEquals(-30.0, dc.getMaxDD(), 0.01);
	}

	@Test
	void shouldIgnoreWithdrawal() {
		DrawDownCalc dc = new DrawDownCalc();
		dc.updateNav(100);
		dc.updateNav(200);
		dc.updateNav(100);
		dc.updateNav(150);
		dc.changeCash(-50);
		assertEquals(-25.0, dc.getCurrentDD(), 0.01);
		assertEquals(-50.0, dc.getMaxDD(), 0.01);
		dc.updateNav(80);
		assertEquals(-40.0, dc.getCurrentDD(), 0.01);
		assertEquals(-50.0, dc.getMaxDD(), 0.01);
	}

	@Test
	void shouldIgnoreDeposit() {
		DrawDownCalc dc = new DrawDownCalc();
		dc.updateNav(100);
		dc.updateNav(200);
		dc.updateNav(100);
		dc.updateNav(150);
		dc.changeCash(50);
		assertEquals(-25.0, dc.getCurrentDD(), 0.01);
		assertEquals(-50.0, dc.getMaxDD(), 0.01);
		dc.updateNav(200);
		assertEquals(-25.0, dc.getCurrentDD(), 0.01);
		assertEquals(-50.0, dc.getMaxDD(), 0.01);
	}

}

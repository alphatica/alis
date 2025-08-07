package com.alphatica.alis.trading.ranking;

import org.junit.jupiter.api.Test;

import static com.alphatica.alis.trading.ranking.SourceIdGenerator.SOURCE_ID;
import static org.junit.jupiter.api.Assertions.*;

class SourceIdGeneratorTest {

	@Test
	void shouldCreateUniqueId() {
		assertNotEquals(SOURCE_ID.next(), SOURCE_ID.next());
	}
}
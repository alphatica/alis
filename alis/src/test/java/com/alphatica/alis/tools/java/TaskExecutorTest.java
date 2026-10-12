package com.alphatica.alis.tools.java;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutorTest {

	@Test
	void shouldReturnNoResultsWithoutTasks() throws Exception {
		// e.g. an empty directory of market files
		assertTrue(new TaskExecutor<String>().getResults().isEmpty());
	}

	@Test
	void shouldReturnResultsInSubmitOrderSkippingNulls() throws Exception {
		TaskExecutor<String> executor = new TaskExecutor<>();
		executor.submit(() -> "a");
		executor.submit(() -> null);
		executor.submit(() -> "b");

		assertEquals(List.of("a", "b"), executor.getResults());
	}
}

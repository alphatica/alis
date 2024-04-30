package com.alphatica.alis.tools.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class GreenLambdaExecutor<R> {
	private final ExecutorService es;

	private final List<Future<R>> futures;

	public GreenLambdaExecutor() {
		futures = new ArrayList<>(1024);
		this.es = Executors.newVirtualThreadPerTaskExecutor();
	}

	public void submit(Supplier<R> supplier) {
		Future<R> f = es.submit(supplier::get);
		futures.add(f);
	}

	public List<R> results() throws ExecutionException, InterruptedException {
		List<R> results = new ArrayList<>(futures.size());
		for (Future<R> f : futures) {
			R r = f.get();
			results.add(r);
		}
		es.close();
		return results;
	}
}

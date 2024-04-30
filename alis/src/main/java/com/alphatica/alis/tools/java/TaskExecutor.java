package com.alphatica.alis.tools.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class TaskExecutor<T> {
	private List<Callable<T>> tasks;

	public void submit(Callable<T> task) {
		if (tasks == null) {
			tasks = new ArrayList<>();
		}
		tasks.add(task);
	}

	public List<T> getResults() throws ExecutionException, InterruptedException {
		List<T> results = new ArrayList<>();
		try(ExecutorService executorService = ForkJoinPool.commonPool()) {
			List<Future<T>> futures = new ArrayList<>();
			for(Callable<T> task : tasks) {
				Future<T> future = executorService.submit(task);
				futures.add(future);
			}
			for(Future<T> future : futures) {
				T t = future.get();
				if (t != null) {
					results.add(t);
				}
			}
			return results;
		}
	}
}

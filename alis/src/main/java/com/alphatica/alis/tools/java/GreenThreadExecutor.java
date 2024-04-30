package com.alphatica.alis.tools.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

public class GreenThreadExecutor<T, R> {

    private final Function<T, R> fun;
    private final ExecutorService es;

    private final List<Future<R>> futures;

    public GreenThreadExecutor(Function<T, R> fun) {
        this.fun = fun;
        this.es = Executors.newVirtualThreadPerTaskExecutor();
        this.futures = new ArrayList<>(1024);
    }

    public void submit(T arg) {
        Future<R> f = es.submit(() -> fun.apply(arg));
        futures.add(f);
    }

    public List<R> results() throws ExecutionException, InterruptedException {
        List<R> results = new ArrayList<>(futures.size());
        for (Future<R> f : futures) {
            results.add(f.get());
        }
        es.close();
        return results;
    }
}

package io.opentracing.contrib.spring.cloud.async.instrument;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;


/**
 * @author kameshsampath
 */
public class TracedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private final Tracer tracer;
    private final ThreadPoolTaskExecutor delegate;

    public TracedThreadPoolTaskExecutor(Tracer tracer, ThreadPoolTaskExecutor delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable task) {
        this.delegate.execute(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        this.delegate.execute(new TracedRunnable(task, activeSpan()), startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.delegate.submit(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.delegate.submit(new TracedCallable<>(task, activeSpan()));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return this.delegate.submitListenable(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return this.delegate.submitListenable(new TracedCallable<>(task, activeSpan()));
    }

    @Override
    public void afterPropertiesSet() {
        this.delegate.afterPropertiesSet();
        super.afterPropertiesSet();
    }

    @Override
    public void destroy() {
        this.delegate.destroy();
        super.destroy();
    }

    @Override
    public void shutdown() {
        this.delegate.shutdown();
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        return this.delegate.getThreadPoolExecutor();
    }

    private ActiveSpan activeSpan() {
        return tracer.activeSpan();
    }
}

package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;


/**
 * @author kameshsampath
 */
public class TracedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private BeanFactory beanFactory;
    private Tracer tracer;
    private ThreadPoolTaskExecutor delegate;

    public TracedThreadPoolTaskExecutor(BeanFactory beanFactory, ThreadPoolTaskExecutor delegate) {
        this.beanFactory = beanFactory;
        this.delegate = delegate;
        this.tracer = this.beanFactory.getBean(Tracer.class);
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

    private ActiveSpan activeSpan() {
        return tracer.activeSpan();
    }

}

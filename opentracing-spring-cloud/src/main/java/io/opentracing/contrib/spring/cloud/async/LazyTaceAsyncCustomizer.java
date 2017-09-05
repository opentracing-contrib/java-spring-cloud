package io.opentracing.contrib.spring.cloud.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import java.util.concurrent.Executor;

public class LazyTaceAsyncCustomizer extends AsyncConfigurerSupport {

    private final BeanFactory beanFactory;
    private final AsyncConfigurer delegate;

    public LazyTaceAsyncCustomizer(BeanFactory beanFactory, AsyncConfigurer delegate) {
        this.beanFactory = beanFactory;
        this.delegate = delegate;
    }

    @Override
    public Executor getAsyncExecutor() {
        return new LazyTraceExecutor(this.beanFactory, this.delegate.getAsyncExecutor());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this.delegate.getAsyncUncaughtExceptionHandler();
    }
}

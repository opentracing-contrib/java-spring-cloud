package io.opentracing.contrib.spring.cloud.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import java.util.concurrent.Executor;

/**
 * @author kameshsampath
 */
public class TraceableAsyncCustomizer extends AsyncConfigurerSupport {

    private final BeanFactory beanFactory;
    private final AsyncConfigurer delegate;

    public TraceableAsyncCustomizer(BeanFactory beanFactory, AsyncConfigurer delegate) {
        this.beanFactory = beanFactory;
        this.delegate = delegate;
    }

    @Override
    public Executor getAsyncExecutor() {
       return new TraceableExecutor(this.beanFactory, this.delegate.getAsyncExecutor());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this.delegate.getAsyncUncaughtExceptionHandler();
    }
}

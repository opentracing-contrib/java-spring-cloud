package io.opentracing.contrib.spring.cloud.async.instrument;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;

/**
 * @author kameshsampath
 */
public class TracedAsyncConfigurer extends AsyncConfigurerSupport {

    private final Tracer tracer;
    private final AsyncConfigurer delegate;

    public TracedAsyncConfigurer(Tracer tracer, AsyncConfigurer delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
    }

    @Override
    public Executor getAsyncExecutor() {
       return new TracedExecutor(this.delegate.getAsyncExecutor(), this.tracer);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return delegate.getAsyncUncaughtExceptionHandler();
    }
}

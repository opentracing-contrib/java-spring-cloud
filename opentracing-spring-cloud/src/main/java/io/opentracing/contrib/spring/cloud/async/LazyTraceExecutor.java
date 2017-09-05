package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedRunnable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * @author kameshs
 */
public class LazyTraceExecutor implements Executor {

    private final BeanFactory beanFactory;
    private final Executor delegate;
    private Tracer tracer;

    public LazyTraceExecutor(BeanFactory beanFactory, Executor delegate) {
        this.beanFactory = beanFactory;
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {

        if (this.tracer == null) {
            try {
                this.tracer = this.beanFactory.getBean(Tracer.class);
            } catch (NoSuchBeanDefinitionException e) {
                this.delegate.execute(command);
                return;
            }
            this.delegate.execute(new TracedRunnable(command, this.tracer.activeSpan()));
        }
    }
}

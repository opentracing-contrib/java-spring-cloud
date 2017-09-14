package io.opentracing.contrib.spring.cloud.async;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * enabling async related processing.
 *
 * @author Dave Syer
 * @author Marcin Grzejszczak
 *
 * @see TracedExecutor
 * @see TraceAsyncAspect
 */
@EnableAsync
@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(CustomAsyncConfigurerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.async.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAsyncAutoConfiguration {

    @Autowired
    private Tracer tracer;

    @Configuration
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    static class DefaultTracedAsyncConfigurerSupport extends AsyncConfigurerSupport {

        @Autowired
        private Tracer tracer;

        @Override
        public Executor getAsyncExecutor() {
            return new TracedExecutor(new SimpleAsyncTaskExecutor(), tracer);
        }
    }

    @Bean
    public ExecutorBeanPostProcessor executorBeanPostProcessor() {
        return new ExecutorBeanPostProcessor(tracer);
    }

    @Bean
    public TraceAsyncAspect traceAsyncAspect() {
        return new TraceAsyncAspect(tracer);
    }

    @Bean
    public TracedAsyncWebAspect tracedAsyncWebAspect() {
        return new TracedAsyncWebAspect(tracer);
    }
}

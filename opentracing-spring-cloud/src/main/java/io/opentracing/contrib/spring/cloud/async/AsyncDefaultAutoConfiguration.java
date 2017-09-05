package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import org.springframework.beans.factory.BeanFactory;
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

import java.util.concurrent.Executor;

/**
 * @author kamesh
 */
@Configuration
@EnableAsync
@ConditionalOnBean(Tracer.class)
public class AsyncDefaultAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    static class DefaultAsyncConfigurerSupport extends AsyncConfigurerSupport {

        @Autowired
        private BeanFactory beanFactory;

        @Override
        public Executor getAsyncExecutor() {
            return new LazyTraceExecutor(this.beanFactory, new SimpleAsyncTaskExecutor());
        }
    }

    @Bean
    public TraceAsyncAspect traceAsyncAspect(Tracer tracer) {
        return new TraceAsyncAspect(tracer);
    }
}

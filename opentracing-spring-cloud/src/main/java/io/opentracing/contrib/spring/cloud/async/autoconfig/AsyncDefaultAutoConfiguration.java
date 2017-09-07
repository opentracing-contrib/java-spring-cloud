package io.opentracing.contrib.spring.cloud.async.autoconfig;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.async.ProxiedExecutor;
import io.opentracing.contrib.spring.cloud.async.TraceAsyncAspect;
import io.opentracing.contrib.spring.cloud.async.TraceableExecutor;
import io.opentracing.contrib.spring.cloud.async.web.TracedAsyncWebAspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

/**
 * @author kameshsampath
 */
@Configuration
@EnableAsync
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(AsyncCustomAutoConfiguration.class)
public class AsyncDefaultAutoConfiguration {

    @Autowired
    private BeanFactory beanFactory;

    @Configuration
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    static class DefaultAsyncConfigurerSupport extends AsyncConfigurerSupport {

        @Autowired
        private BeanFactory beanFactory;

        @Autowired
        private Tracer tracer;

        @Override
        public Executor getAsyncExecutor() {
            return new TraceableExecutor(this.beanFactory, new SimpleAsyncTaskExecutor());
        }
    }

    @Bean
    public TraceAsyncAspect traceAsyncAspect() {
        return new TraceAsyncAspect();
    }

    @Bean
    public TracedAsyncWebAspect tracedAsyncWebAspect(){
        return new TracedAsyncWebAspect();
    }

    @Bean
    public ProxiedExecutor proxiedExecutor() {
        return new ProxiedExecutor(beanFactory);
    }
}

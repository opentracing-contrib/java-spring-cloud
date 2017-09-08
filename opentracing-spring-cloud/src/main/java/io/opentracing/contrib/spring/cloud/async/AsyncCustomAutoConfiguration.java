package io.opentracing.contrib.spring.cloud.async;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.async.instrument.TracedAsyncConfigurer;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * that wraps an existing custom {@link AsyncConfigurer} in a {@link TracedAsyncConfigurer}
 *
 * @author Dave Syer
 */
@Configuration
@ConditionalOnBean(AsyncConfigurer.class)
//TODO when Scheduling is added we need to do @AutoConfigurationAfter on it
@AutoConfigureBefore(AsyncDefaultAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncCustomAutoConfiguration implements BeanPostProcessor {

    @Autowired
    private Tracer tracer;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AsyncConfigurer) {
            AsyncConfigurer configurer = (AsyncConfigurer) bean;
            return new TracedAsyncConfigurer(tracer, configurer);
        }
        return bean;
    }
}

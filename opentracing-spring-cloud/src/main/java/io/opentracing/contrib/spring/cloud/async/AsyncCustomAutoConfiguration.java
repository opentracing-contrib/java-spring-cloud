package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * @author kameshsampath
 */
@Configuration
@ConditionalOnBean(AsyncConfigurer.class)
@AutoConfigureBefore(AsyncDefaultAutoConfiguration.class)
//TODO when Scheduling is added we need to do @AutoConfigurationAfter here
public class AsyncCustomAutoConfiguration implements BeanPostProcessor {

    @Autowired
    private BeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AsyncConfigurer) {
            AsyncConfigurer configurer = (AsyncConfigurer) bean;
            return new TraceableAsyncCustomizer(this.beanFactory, configurer);
        }
        return bean;
    }


    protected Tracer getTracer(){
        return  this.beanFactory.getBean(Tracer.class);
    }
}

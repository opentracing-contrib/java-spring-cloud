package io.opentracing.contrib.spring.cloud.feign;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.feign.FeignContext;

import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
public class FeignContextBeanPostProcessor implements BeanPostProcessor {

  private Tracer tracer;
  private BeanFactory beanFactory;

  FeignContextBeanPostProcessor(Tracer tracer, BeanFactory beanFactory) {
    this.tracer = tracer;
    this.beanFactory = beanFactory;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
    if (bean instanceof FeignContext && !(bean instanceof TraceFeignContext)) {
      return new TraceFeignContext(tracer, (FeignContext) bean, beanFactory);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
    return bean;
  }
}

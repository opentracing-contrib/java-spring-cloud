package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.Request;
import feign.opentracing.TracingClient;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
import io.opentracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.feign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pavol Loffay
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnClass(Client.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class FeignTracingAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @Bean
  FeignContextBeanPostProcessor feignContextBeanPostProcessor(BeanFactory beanFactory) {
    return new FeignContextBeanPostProcessor(tracer, beanFactory);
  }

  @Configuration
  @ConditionalOnClass(name = {"com.netflix.hystrix.HystrixCommand", "feign.hystrix.HystrixFeign"})
  @ConditionalOnProperty(name = "feign.hystrix.enabled", havingValue = "true")
  public static class HystrixFeign {
    @Autowired
    public HystrixFeign(Tracer tracer) {
      TracingConcurrencyStrategy.register(tracer);
    }
  }

  @Bean
  public TracingAspect tracingAspect() {
    return new TracingAspect();
  }

  /**
   * Trace feign clients created manually
   */
  @Aspect
  class TracingAspect {
    @Around("execution (* feign.Client.*(..)) && !within(is(FinalType))")
    public Object feignClientWasCalled(final ProceedingJoinPoint pjp) throws Throwable {
      Object bean = pjp.getTarget();
      if (!(bean instanceof TracingClient)) {
        Object[] args = pjp.getArgs();
        return new TracingClient((Client) bean, tracer).execute((Request) args[0], (Request.Options) args[1]);
      }
      return pjp.proceed();
    }
  }
}

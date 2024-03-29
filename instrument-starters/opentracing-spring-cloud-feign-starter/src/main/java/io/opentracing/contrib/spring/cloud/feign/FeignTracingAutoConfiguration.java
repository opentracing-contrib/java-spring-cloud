/**
 * Copyright 2017-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.Request;
import feign.opentracing.FeignSpanDecorator;
import feign.opentracing.TracingClient;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Pavol Loffay
 * @author Eddú Meléndez
 * @author Gert Dubois
 */
@Configuration
@ConditionalOnClass(Client.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@AutoConfigureBefore(name = "org.springframework.cloud.openfeign.FeignAutoConfiguration")
@ConditionalOnProperty(name = "opentracing.spring.cloud.feign.enabled", havingValue = "true", matchIfMissing = true)
public class FeignTracingAutoConfiguration {

  @Autowired
  @Lazy
  private Tracer tracer;

  @Autowired(required = false)
  @Lazy
  private List<FeignSpanDecorator> spanDecorators;

  @Bean
  @ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignContext")
  FeignContextBeanPostProcessor feignContextBeanPostProcessor() {
    return new FeignContextBeanPostProcessor(tracer, spanDecorators);
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
        return new TracingClientBuilder((Client) bean, tracer)
            .withFeignSpanDecorators(spanDecorators)
            .build()
            .execute((Request) args[0], (Request.Options) args[1]);
      }
      return pjp.proceed();
    }
  }
}

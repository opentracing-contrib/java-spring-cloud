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
import feign.opentracing.FeignSpanDecorator;
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.context.annotation.Lazy;

class TracedFeignBeanFactory {

  private final Tracer tracer;
  private final BeanFactory beanFactory;
  private final List<FeignSpanDecorator> spanDecorators;

  public TracedFeignBeanFactory(Tracer tracer, BeanFactory beanFactory, @Lazy List<FeignSpanDecorator> spanDecorators) {
    this.tracer = tracer;
    this.beanFactory = beanFactory;
    this.spanDecorators = spanDecorators;
  }

  public Object from(Object bean) {
    if (bean instanceof TracingClient || bean instanceof LoadBalancedTracedFeign) {
      return bean;
    }

    if (bean instanceof Client) {
      if (bean instanceof LoadBalancerFeignClient) {
        return new LoadBalancedTracedFeign(
            buildTracingClient(((LoadBalancerFeignClient) bean).getDelegate(), tracer),
            beanFactory.getBean(CachingSpringLoadBalancerFactory.class),
            beanFactory.getBean(SpringClientFactory.class));
      }
      return buildTracingClient((Client) bean, tracer);
    }

    return bean;
  }


  private TracingClient buildTracingClient(Client delegate, Tracer tracer) {
    return new io.opentracing.contrib.spring.cloud.feign.TracingClientBuilder(delegate, tracer)
        .withFeignSpanDecorators(spanDecorators)
        .build();
  }

  /**
   * Needed for cast in {@link org.springframework.cloud.openfeign.FeignClientFactoryBean}
   */
  static class LoadBalancedTracedFeign extends LoadBalancerFeignClient {

    public LoadBalancedTracedFeign(Client delegate,
                                   CachingSpringLoadBalancerFactory lbClientFactory,
                                   SpringClientFactory clientFactory) {
      super(delegate, lbClientFactory, clientFactory);
    }
  }

}

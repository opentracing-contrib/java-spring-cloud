/**
 * Copyright 2017 The OpenTracing Authors
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
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.netflix.feign.FeignContext;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

/**
 * @author Pavol Loffay
 */
public class TraceFeignContext extends FeignContext {

  private FeignContext delegate;
  private Tracer tracer;
  private BeanFactory beanFactory;

  TraceFeignContext(Tracer tracer, FeignContext delegate, BeanFactory beanFactory) {
    this.delegate = delegate;
    this.tracer = tracer;
    this.beanFactory = beanFactory;
  }

  @Override
  public <T> T getInstance(String name, Class<T> type) {
    T object = this.delegate.getInstance(name, type);
    return (T) this.addTracingClient(object);
  }

  @Override
  public <T> Map<String, T> getInstances(String name, Class<T> type) {
    Map<String, T> instances = this.delegate.getInstances(name, type);
    if (instances == null) {
      return null;
    }
    Map<String, T> tracedInstances = new HashMap<>();
    for (Map.Entry<String, T> instanceEntry : instances.entrySet()) {
      tracedInstances
          .put(instanceEntry.getKey(), (T) this.addTracingClient(instanceEntry.getValue()));
    }
    return tracedInstances;
  }

  private Object addTracingClient(Object bean) {
    if (bean instanceof TracingClient || bean instanceof LoadBalancedTracedFeign) {
      return bean;
    }

    if (bean instanceof Client) {
      if (bean instanceof LoadBalancerFeignClient && !(bean instanceof LoadBalancedTracedFeign)) {
        return new LoadBalancedTracedFeign(
            new TracingClient(((LoadBalancerFeignClient) bean).getDelegate(), tracer),
            beanFactory.getBean(CachingSpringLoadBalancerFactory.class),
            beanFactory.getBean(SpringClientFactory.class));
      }
      return new TracingClient((Client) bean, tracer);
    }

    return bean;
  }

  /**
   * Needed for cast in {@link org.springframework.cloud.netflix.feign.FeignClientFactoryBean}
   */
  static class LoadBalancedTracedFeign extends LoadBalancerFeignClient {

    public LoadBalancedTracedFeign(Client delegate,
        CachingSpringLoadBalancerFactory lbClientFactory,
        SpringClientFactory clientFactory) {
      super(delegate, lbClientFactory, clientFactory);
    }
  }
}

/*
 * Copyright 2017-2021 The OpenTracing Authors
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
import io.opentracing.Tracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignContext;

/**
 * @author Pavol Loffay
 */
class TraceFeignContext extends FeignContext {

  private final FeignContext delegate;
  private final TracedFeignBeanFactory tracedFeignBeanFactory;

  TraceFeignContext(Tracer tracer, FeignContext delegate, List<FeignSpanDecorator> spanDecorators) {
    this.delegate = delegate;
    this.tracedFeignBeanFactory = new TracedFeignBeanFactory(tracer, spanDecorators);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getInstance(String name, Class<T> type) {
    T object = this.delegate.getInstance(name, type);
    if (object == null && type.isAssignableFrom(Client.class)) {
      object = (T) new Client.Default(null, null);
    }
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
    return tracedFeignBeanFactory.from(bean);
  }

}

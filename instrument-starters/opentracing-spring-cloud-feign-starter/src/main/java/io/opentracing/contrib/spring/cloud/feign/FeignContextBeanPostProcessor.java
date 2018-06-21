/**
 * Copyright 2017-2018 The OpenTracing Authors
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

import feign.opentracing.FeignSpanDecorator;
import io.opentracing.Tracer;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.netflix.feign.FeignContext;

/**
 * @author Pavol Loffay
 */
public class FeignContextBeanPostProcessor implements BeanPostProcessor {

  private Tracer tracer;
  private BeanFactory beanFactory;
  private List<FeignSpanDecorator> spanDecorators;

  FeignContextBeanPostProcessor(Tracer tracer, BeanFactory beanFactory,
      List<FeignSpanDecorator> spanDecorators) {
    this.tracer = tracer;
    this.beanFactory = beanFactory;
    this.spanDecorators = spanDecorators;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
    if (bean instanceof FeignContext && !(bean instanceof TraceFeignContext)) {
      return new TraceFeignContext(tracer, (FeignContext) bean, beanFactory, spanDecorators);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
    return bean;
  }
}

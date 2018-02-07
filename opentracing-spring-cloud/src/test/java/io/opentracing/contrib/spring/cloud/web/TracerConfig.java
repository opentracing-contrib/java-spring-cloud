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
package io.opentracing.contrib.spring.cloud.web;

import io.opentracing.Tracer;

import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracerConfig implements BeanPostProcessor {

  @Override
  public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
    if (bean instanceof Tracer) {
      return Mockito.mock(Tracer.class);
    }
    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
    return bean;
  }
    
}

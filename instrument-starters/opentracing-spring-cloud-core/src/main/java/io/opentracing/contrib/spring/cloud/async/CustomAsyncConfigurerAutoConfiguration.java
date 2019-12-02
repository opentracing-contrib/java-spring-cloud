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
package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.async.instrument.TracedAsyncConfigurer;

import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * This auto-configuration wraps available {@link AsyncConfigurer} in a {@link
 * TracedAsyncConfigurer}
 *
 * @author Pavol Loffay
 * @author Tadaya Tsuyukubo
 */
@Configuration
@ConditionalOnBean(AsyncConfigurer.class)
//TODO when Scheduling is added we need to do @AutoConfigurationAfter on it
@AutoConfigureBefore(DefaultAsyncAutoConfiguration.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.async.enabled", havingValue = "true", matchIfMissing = true)
public class CustomAsyncConfigurerAutoConfiguration implements BeanPostProcessor, PriorityOrdered, BeanFactoryAware {

  private BeanFactory beanFactory;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof AsyncConfigurer && !(bean instanceof TracedAsyncConfigurer)) {
      AsyncConfigurer configurer = (AsyncConfigurer) bean;
      Tracer tracer = this.beanFactory.getBean(Tracer.class);
      return new TracedAsyncConfigurer(tracer, configurer);
    }
    return bean;
  }

  @Override
  public int getOrder() {
    return PriorityOrdered.LOWEST_PRECEDENCE;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

}

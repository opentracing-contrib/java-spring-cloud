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
package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * This auto-configuration provides all necessary bean to instrument @Async. AsyncWebTask/Callable
 * and provided or default Executors.
 */
@EnableAsync
@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter({CustomAsyncConfigurerAutoConfiguration.class, TracerAutoConfiguration.class})
@ConditionalOnProperty(name = "opentracing.spring.cloud.async.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAsyncAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @Configuration
  @ConditionalOnMissingBean(AsyncConfigurer.class)
  static class DefaultTracedAsyncConfigurerSupport extends AsyncConfigurerSupport {

    @Autowired
    private Tracer tracer;

    @Override
    public Executor getAsyncExecutor() {
      return new TracedExecutor(new SimpleAsyncTaskExecutor(), tracer);
    }
  }

  @Bean
  public ExecutorBeanPostProcessor executorBeanPostProcessor() {
    return new ExecutorBeanPostProcessor(tracer);
  }

  @Bean
  public TraceAsyncAspect traceAsyncAspect() {
    return new TraceAsyncAspect(tracer);
  }

  @Bean
  public TracedAsyncWebAspect tracedAsyncWebAspect() {
    return new TracedAsyncWebAspect(tracer);
  }
}

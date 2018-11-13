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
package io.opentracing.contrib.spring.cloud.traced;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.aop.MethodInterceptorSpanDecorator;
import io.opentracing.contrib.spring.cloud.aop.MethodInterceptorSpanDecoratorAutoconfiguration;
import io.opentracing.contrib.spring.cloud.traced.TracedAspect;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.CollectionUtils;

@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter({TracerAutoConfiguration.class,
    MethodInterceptorSpanDecoratorAutoconfiguration.class})
@ConditionalOnProperty(name = "tm.opentracing.aop.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
@ConditionalOnClass({ProceedingJoinPoint.class})
public class AopTracedAutoConfiguration {

  private final ObjectProvider<List<MethodInterceptorSpanDecorator>> methodInterceptorSpanDecorators;

  public AopTracedAutoConfiguration(
      ObjectProvider<List<MethodInterceptorSpanDecorator>> methodInterceptorSpanDecorators) {
    this.methodInterceptorSpanDecorators = methodInterceptorSpanDecorators;
  }

  @Bean
  @ConditionalOnMissingBean
  public TracedAspect tracingMethodAspect(Tracer tracer) {
    List<MethodInterceptorSpanDecorator> spanDecorators = new ArrayList<>();
    spanDecorators.add(new MethodInterceptorSpanDecorator.StandardTags());

    List<MethodInterceptorSpanDecorator> decorators = this.methodInterceptorSpanDecorators
        .getIfAvailable();
    if (!CollectionUtils.isEmpty(decorators)) {
      decorators = new ArrayList<>(decorators);
      AnnotationAwareOrderComparator.sort(decorators);
      spanDecorators.addAll(decorators);
    }

    return new TracedAspect(tracer, spanDecorators);
  }

}

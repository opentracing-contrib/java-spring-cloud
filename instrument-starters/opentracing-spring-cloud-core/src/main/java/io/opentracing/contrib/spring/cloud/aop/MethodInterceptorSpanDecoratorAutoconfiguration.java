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
package io.opentracing.contrib.spring.cloud.aop;

import io.opentracing.contrib.mdc.MDCSpanDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = {"opentracing.spring.cloud.aop.enabled"}, matchIfMissing = true)
@EnableConfigurationProperties(AopTracingProperties.class)
public class MethodInterceptorSpanDecoratorAutoconfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = "opentracing.spring.cloud.aop.mdc.enabled", matchIfMissing = true)
  public MethodInterceptorSpanDecorator methodInterceptorSpanDecorator(
      AopTracingProperties aopTracingProperties) {
    return new MethodInterceptorMDCSpanDecorator(
        new MDCSpanDecorator(aopTracingProperties.getMdc().getEntries(),
            aopTracingProperties.getMdc().getBaseTagKey()));
  }

}

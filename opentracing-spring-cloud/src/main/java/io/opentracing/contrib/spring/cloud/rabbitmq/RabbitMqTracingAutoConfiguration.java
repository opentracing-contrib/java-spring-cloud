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
package io.opentracing.contrib.spring.cloud.rabbitmq;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.Assert;

/**
 * Auto-configuration for RabbitMq tracing.
 *
 * @author Gilles Robert
 */
@Configuration
@ConditionalOnClass({Message.class, RabbitTemplate.class})
@ConditionalOnBean({Tracer.class, RabbitTemplate.class})
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.rabbitmq.enabled", havingValue = "true", matchIfMissing = true
)
public class RabbitMqTracingAutoConfiguration {

  @Autowired @Lazy private Tracer tracer;

  @Bean
  public RabbitMqSendTracingAspect rabbitMqSendTracingAspect(RabbitTemplate rabbitTemplate) {
    Assert.notNull(
        rabbitTemplate.getMessageConverter(), "RabbitTemplate has no message converter configured");
    return new RabbitMqSendTracingAspect(tracer, rabbitTemplate.getMessageConverter());
  }

  @Bean
  public RabbitMqReceiveTracingInterceptor rabbitMqReceiveTracingInterceptor() {
    return new RabbitMqReceiveTracingInterceptor(tracer);
  }

  @Bean
  public RabbitMqBeanPostProcessor rabbitMqBeanPostProcessor(
      RabbitMqReceiveTracingInterceptor interceptor) {
    return new RabbitMqBeanPostProcessor(interceptor);
  }
}

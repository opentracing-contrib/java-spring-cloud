/**
 * Copyright 2017-2020 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.kafka;

import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.spring.TracingConsumerFactory;
import io.opentracing.contrib.kafka.spring.TracingKafkaAspect;
import io.opentracing.contrib.kafka.spring.TracingProducerFactory;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ConditionalOnClass(ProducerFactory.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.kafka.enabled", havingValue = "true", matchIfMissing = true)
class KafkaAutoConfiguration {

  @Bean
  public BeanPostProcessor kafkaProducerPostProcessor(Tracer tracer) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ProducerFactory && !(bean instanceof TracingProducerFactory)) {
          return new TracingProducerFactory((ProducerFactory)bean, tracer);
        }
        return bean;
      }
    };
  }

  @Bean
  public BeanPostProcessor kafkaConsumerPostProcessor(Tracer tracer) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ConsumerFactory && !(bean instanceof TracingConsumerFactory)) {
          return new TracingConsumerFactory((ConsumerFactory) bean, tracer);
        }
        return bean;
      }
    };
  }

  @Bean
  @ConditionalOnClass(ProxyFactoryBean.class)
  public TracingKafkaAspect tracingKafkaAspect(Tracer tracer) {
    return new TracingKafkaAspect(tracer);
  }
}

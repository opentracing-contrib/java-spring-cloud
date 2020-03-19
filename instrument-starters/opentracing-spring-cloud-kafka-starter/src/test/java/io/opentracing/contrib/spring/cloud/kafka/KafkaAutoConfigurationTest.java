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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.spring.TracingConsumerFactory;
import io.opentracing.contrib.kafka.spring.TracingProducerFactory;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaAutoConfigurationTest {

  @Test
  public void loadKafkaTracingProducerFactory() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TracerConfig.class, FakeKafkaConfig.class, KafkaAutoConfiguration.class);
    context.refresh();
    ProducerFactory tracingProducerFactory = context.getBean(ProducerFactory.class);
    assertTrue(tracingProducerFactory instanceof TracingProducerFactory);
  }

  @Test
  public void loadNormalProducerFactoryWhenDisabled() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TracerConfig.class, FakeKafkaConfig.class, KafkaAutoConfiguration.class);
    TestPropertyValues.of("opentracing.spring.cloud.kafka.enabled:false").applyTo(context);
    context.refresh();
    ProducerFactory tracingProducerFactory = context.getBean(ProducerFactory.class);
    assertFalse(tracingProducerFactory instanceof TracingProducerFactory);
  }

  @Test
  public void loadTracingConsumerFactory() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TracerConfig.class, FakeKafkaConfig.class, KafkaAutoConfiguration.class);
    context.refresh();
    ConsumerFactory tracingConsumerFactory = context.getBean(ConsumerFactory.class);
    assertTrue(tracingConsumerFactory instanceof TracingConsumerFactory);
  }

  @Test
  public void loadNormalConsumerFactoryWhenDisabled() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(TracerConfig.class, FakeKafkaConfig.class, KafkaAutoConfiguration.class);
    TestPropertyValues.of("opentracing.spring.cloud.kafka.enabled:false").applyTo(context);
    context.refresh();
    ConsumerFactory consumerFactory = context.getBean(ConsumerFactory.class);
    assertFalse(consumerFactory instanceof TracingConsumerFactory);
  }

  @Test
  public void loadNormalConsumerFactoryWhenTracerNotPresent() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(FakeKafkaConfig.class, KafkaAutoConfiguration.class);
    context.refresh();
    ConsumerFactory consumerFactory = context.getBean(ConsumerFactory.class);
    assertFalse(consumerFactory instanceof TracingConsumerFactory);
  }




  @Configuration
  static class TracerConfig {

    @Bean
    public Tracer tracer() {
      return mock(Tracer.class);
    }
  }

  static class FakeKafkaConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
      return new ProducerFactory<String, String>() {
        @Override
        public Producer<String, String> createProducer() {
          return null;
        }
      };
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
      return new ConsumerFactory<String, String>() {
        @Override
        public Consumer<String, String> createConsumer() {
          return null;
        }

        @Override
        public Consumer<String, String> createConsumer(String clientIdSuffix) {
          return null;
        }

        @Override
        public Consumer<String, String> createConsumer(String groupId, String clientIdSuffix) {
          return null;
        }

        @Override
        public Consumer<String, String> createConsumer(String s, String s1, String s2) {
          return null;
        }

        @Override
        public Consumer<String, String> createConsumer(String groupId, String clientIdPrefix, String clientIdSuffix, Properties properties) {
          return null;
        }

        @Override
        public boolean isAutoCommit() {
          return false;
        }

        @Override
        public Map<String, Object> getConfigurationProperties() {
          return null;
        }

        @Override
        public Deserializer<String> getKeyDeserializer() {
          return null;
        }

        @Override
        public Deserializer<String> getValueDeserializer() {
          return null;
        }
      };
    }
  }
}

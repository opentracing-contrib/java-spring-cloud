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
package io.opentracing.contrib.spring.cloud.mongo;

import static org.mockito.Mockito.mock;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vivien Maleze
 */
public class MongoTracingAutoConfigurationTest {

  @Test
  public void createsTracingPostProcessor() {
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues("spring.data.mongodb.port=27017") // Otherwise a random embedded mongo port is used
        .withConfiguration(UserConfigurations.of(TracerConfig.class, MongoConfig.class))
        .withConfiguration(AutoConfigurations.of(
            MongoTracingAutoConfiguration.class,
            EmbeddedMongoAutoConfiguration.class
        ));

    contextRunner.run(context -> Assertions.assertThat(context).hasSingleBean(TracingMongoClientPostProcessor.class));
  }

  @Test
  public void doesNotCreateTracingPostProcessorWhenNoTracer() {
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(MongoConfig.class))
        .withConfiguration(AutoConfigurations.of(MongoTracingAutoConfiguration.class));

    contextRunner.run(context -> Assertions.assertThat(context).doesNotHaveBean(TracingMongoClientPostProcessor.class));
  }

  @Test
  public void doesNotCreateTracingPostProcessorWhenNoMongoClient() {
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(UserConfigurations.of(TracerConfig.class))
        .withConfiguration(AutoConfigurations.of(MongoTracingAutoConfiguration.class));

    contextRunner.run(context -> Assertions.assertThat(context).doesNotHaveBean(TracingMongoClientPostProcessor.class));
  }

  @Test
  public void doesNotCreateTracingPostProcessorWhenDisabled() {
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues("opentracing.spring.cloud.mongo.enabled=false")
        .withConfiguration(UserConfigurations.of(TracerConfig.class, MongoConfig.class))
        .withConfiguration(AutoConfigurations.of(MongoTracingAutoConfiguration.class));

    contextRunner.run(context -> Assertions.assertThat(context).doesNotHaveBean(TracingMongoClientPostProcessor.class));
  }

  @Test
  public void createsTracingPostProcessorWhenAutoConfigured() {
    final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues("spring.data.mongodb.port=27017") // Otherwise a random embedded mongo port is used
        .withConfiguration(AutoConfigurations.of(
            MongoTracingAutoConfiguration.class,
            TracerAutoConfiguration.class,
            MongoAutoConfiguration.class,
            EmbeddedMongoAutoConfiguration.class
        ));

    contextRunner.run(context -> Assertions.assertThat(context).hasSingleBean(TracingMongoClientPostProcessor.class));
  }

  @Configuration
  static class TracerConfig {

    @Bean
    public Tracer tracer() {
      return mock(Tracer.class);
    }
  }

  @Configuration
  static class MongoConfig {

    @Bean
    public MongoClient client() {
      return new MongoClient(new MongoClientURI("mongodb://localhost/test"));
    }
  }
}

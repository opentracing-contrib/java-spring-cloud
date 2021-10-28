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

import com.mongodb.MongoClient;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vivien Maleze
 */
@Configuration
@AutoConfigureAfter({ TracerAutoConfiguration.class, MongoAutoConfiguration.class })
@ConditionalOnBean({ Tracer.class, MongoClient.class })
@ConditionalOnProperty(name = "opentracing.spring.cloud.mongo.enabled", havingValue = "true", matchIfMissing = true)
public class MongoTracingAutoConfiguration {

  private final Tracer tracer;

  public MongoTracingAutoConfiguration(final Tracer tracer) {
    this.tracer = tracer;
  }

  @Bean
  TracingMongoClientPostProcessor tracingMongoClientPostProcessor() {
    return new TracingMongoClientPostProcessor(tracer);
  }
}

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
package io.opentracing.contrib.spring.cloud.redis;

import io.opentracing.contrib.spring.tracer.configuration.TracerRegisterAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Loads the integration with OpenTracing Redis if it's included in the classpath.
 *
 * @author Daniel del Castillo
 */
@Configuration
@AutoConfigureAfter({TracerRegisterAutoConfiguration.class, org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class})
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {

  @Bean
  public RedisAspect openTracingRedisAspect() {
    return new RedisAspect();
  }

}

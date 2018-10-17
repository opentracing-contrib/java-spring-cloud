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
package io.opentracing.contrib.spring.cloud.jdbc;

import io.opentracing.contrib.spring.tracer.configuration.TracerRegisterAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the integration with OpenTracing JDBC if it's included in the classpath.
 *
 * @author Juraci Paixão Kröhling
 */
@Configuration
@AutoConfigureAfter(TracerRegisterAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.jdbc.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JdbcTracingProperties.class)
public class JdbcAutoConfiguration {

  @Bean
  public JdbcAspect jdbcAspect(JdbcTracingProperties jdbcTracingProperties) {
    return new JdbcAspect(jdbcTracingProperties.isWithActiveSpanOnly(),
        jdbcTracingProperties.getIgnoreStatements());
  }
}

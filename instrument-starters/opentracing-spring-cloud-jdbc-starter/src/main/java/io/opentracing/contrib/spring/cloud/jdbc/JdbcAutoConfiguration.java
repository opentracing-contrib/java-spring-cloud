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

import java.util.HashSet;
import java.util.Set;
import io.opentracing.contrib.spring.tracer.configuration.TracerRegisterAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(prefix = "opentracing.spring.cloud.jdbc")
public class JdbcAutoConfiguration {

  private boolean withActiveSpanOnly = false;
  private Set<String> ignoreStatements = new HashSet<>();

  public boolean isWithActiveSpanOnly() {
    return withActiveSpanOnly;
  }

  public void setWithActiveSpanOnly(boolean withActiveSpanOnly) {
    this.withActiveSpanOnly = withActiveSpanOnly;
  }

  public Set<String> getIgnoreStatements() {
    return ignoreStatements;
  }

  public void setIgnoreStatements(Set<String> ignoreStatements) {
    this.ignoreStatements = ignoreStatements;
  }

  @Bean
  public JdbcAspect jdbcAspect() {
    return new JdbcAspect(withActiveSpanOnly, ignoreStatements);
  }
}

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
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentracing.spring.cloud.jdbc")
public class JdbcTracingProperties {

  /**
   * Enable tracing for {@link java.sql.Connection}
   */
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

}

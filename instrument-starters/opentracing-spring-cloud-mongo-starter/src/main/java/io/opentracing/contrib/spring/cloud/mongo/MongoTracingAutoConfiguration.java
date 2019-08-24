/**
 * Copyright 2017-2019 The OpenTracing Authors
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

import io.opentracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vivien Maleze
 */
@Configuration
@ConditionalOnProperty(name = "opentracing.spring.cloud.mongo.enabled", havingValue = "true", matchIfMissing = true)
public class MongoTracingAutoConfiguration {

  private final Tracer tracer;

  public MongoTracingAutoConfiguration(final Tracer tracer) {
    this.tracer = tracer;
  }

}

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
package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.opentracing.FeignSpanDecorator;
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;

import java.util.List;

/**
 * @author Emerson Oliveira
 */
class TracingClientBuilder {

  private Client delegate;
  private Tracer tracer;
  private List<FeignSpanDecorator> decorators;

  TracingClientBuilder(Client delegate, Tracer tracer) {
    this.delegate = delegate;
    this.tracer = tracer;
  }

  io.opentracing.contrib.spring.cloud.feign.TracingClientBuilder withFeignSpanDecorators(List<FeignSpanDecorator> decorators) {
    this.decorators = decorators;
    return this;
  }

  TracingClient build() {
    if (decorators == null || decorators.isEmpty()) {
      return new TracingClient(delegate, tracer);
    }
    return new TracingClient(delegate, tracer, decorators);
  }

}

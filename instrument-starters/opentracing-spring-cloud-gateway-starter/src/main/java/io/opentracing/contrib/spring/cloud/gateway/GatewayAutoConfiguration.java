/**
 * Copyright 2017-2018 The OpenTracing Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.gateway;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.gateway.propagation.TextMapAdapter;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import io.opentracing.propagation.Format;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnClass(GatewayFilter.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.gateway.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayAutoConfiguration {
  @Bean
  public GlobalFilter traceIdTransfer(Tracer tracer) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();
      ServerHttpRequest.Builder requestBuilder = request.mutate();

      tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS,
          new TextMapAdapter(request, requestBuilder));

      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    };
  }
}

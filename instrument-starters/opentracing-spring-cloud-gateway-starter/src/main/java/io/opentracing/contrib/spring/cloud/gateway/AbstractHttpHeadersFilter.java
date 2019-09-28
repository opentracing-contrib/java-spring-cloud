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
package io.opentracing.contrib.spring.cloud.gateway;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * AbstractHttpHeadersFilter
 *
 * @author Weichao Li (liweichao0102@gmail.com)
 * @since 2019/9/28
 */
public abstract class AbstractHttpHeadersFilter implements HttpHeadersFilter {

  protected static final String SPAN_ATTRIBUTE = Span.class.getName();

  protected static final String ROUTE_ATTRIBUTE = ServerWebExchangeUtils.class.getName() + ".gatewayRoute";

  protected final Tracer tracer;

  protected AbstractHttpHeadersFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  public String path(ServerHttpRequest.Builder request) {
    return request.build().getPath().value();
  }

}

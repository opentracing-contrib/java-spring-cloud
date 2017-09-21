/**
 * Copyright 2017 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;

class TracePreZuulFilter extends ZuulFilter {

  static final String COMPONENT_NAME = "zuul";
  static final String CONTEXT_SPAN_KEY = TracePreZuulFilter.class.getName();

  private final Tracer tracer;

  TracePreZuulFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public String filterType() {
    // TODO: replace with org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE
    return "pre";
  }

  @Override
  public int filterOrder() {
    return 0;
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  public Object run() {
    RequestContext ctx = RequestContext.getCurrentContext();

    // span is a child of one created in servlet-filter
    Span span = tracer.buildSpan(ctx.getRequest().getMethod())
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
        .startManual();

    tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
        new TextMapInjectAdapter(ctx.getZuulRequestHeaders()));

    ctx.set(CONTEXT_SPAN_KEY, span);

    return null;
  }


}

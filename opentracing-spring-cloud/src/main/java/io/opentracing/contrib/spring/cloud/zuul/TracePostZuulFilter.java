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
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;

public class TracePostZuulFilter extends ZuulFilter {

  static final String ROUTE_HOST_TAG = "route.host";

  @Override
  public String filterType() {
    // TODO: replace with org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE
    return "post";
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

    Object spanObject = ctx.get(TracePreZuulFilter.CONTEXT_SPAN_KEY);
    if (spanObject instanceof Span) {
      Span span = (Span) spanObject;
      span.setTag(Tags.HTTP_STATUS.getKey(), ctx.getResponseStatusCode());

      if (ctx.getThrowable() != null) {
        onError(ctx.getThrowable(), span);
      } else {
        Object error = ctx.get("error.exception");
        if (error instanceof Exception) {
          onError((Exception) error, span);
        }
      }

      if (ctx.getRouteHost() != null) {
        span.setTag(ROUTE_HOST_TAG, ctx.getRouteHost().toString());
      }

      span.finish();
    }

    return null;
  }

  private static void onError(Throwable throwable, Span span) {
    Tags.ERROR.set(span, Boolean.TRUE);

    if (throwable != null) {
      span.log(errorLogs(throwable));
    }
  }

  private static Map<String, Object> errorLogs(Throwable throwable) {
    Map<String, Object> errorLogs = new HashMap<>(2);
    errorLogs.put("event", Tags.ERROR.getKey());
    errorLogs.put("error.object", throwable);
    return errorLogs;
  }
}

/**
 * Copyright 2017-2020 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.async.instrument;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * @author MiNG
 */
public class TracedListenableFutureCallback<T> implements ListenableFutureCallback<T> {

  private final SuccessCallback<T> successDelegate;
  private final FailureCallback failureDelegate;
  private final Tracer tracer;
  private final Span span;

  public TracedListenableFutureCallback(ListenableFutureCallback<T> delegate, Tracer tracer) {
    this(delegate, delegate, tracer, tracer.activeSpan());
  }

  public TracedListenableFutureCallback(ListenableFutureCallback<T> delegate, Tracer tracer, Span span) {
    this(delegate, delegate, tracer, span);
  }

  public TracedListenableFutureCallback(SuccessCallback<T> successDelegate, FailureCallback failureDelegate, Tracer tracer) {
    this(successDelegate, failureDelegate, tracer, tracer.activeSpan());
  }

  public TracedListenableFutureCallback(@Nullable SuccessCallback<T> successDelegate, @Nullable FailureCallback failureDelegate, Tracer tracer, Span span) {
    Assert.notNull(successDelegate, "'successDelegate' must not be null");
    Assert.notNull(failureDelegate, "'failureDelegate' must not be null");
    this.successDelegate = successDelegate;
    this.failureDelegate = failureDelegate;
    this.tracer = tracer;
    this.span = span;
  }

  @Override
  public void onSuccess(T result) {
    try (Scope ignored = span == null ? null : tracer.scopeManager().activate(span)) {
      successDelegate.onSuccess(result);
    }
  }

  @Override
  public void onFailure(Throwable ex) {
    try (Scope ignored = span == null ? null : tracer.scopeManager().activate(span)) {
      failureDelegate.onFailure(ex);
    }
  }
}

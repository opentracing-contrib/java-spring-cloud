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

import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * @author MiNG
 */
public class TracedListenableFuture<T> implements ListenableFuture<T> {

  private final ListenableFuture<T> delegate;
  private final Tracer tracer;
  private final Span span;

  public TracedListenableFuture(ListenableFuture<T> delegate, Tracer tracer) {
    this.delegate = delegate;
    this.tracer = tracer;
    this.span = tracer.activeSpan();
  }

  @Override
  public void addCallback(ListenableFutureCallback<? super T> callback) {
    delegate.addCallback(new TracedListenableFutureCallback<>(callback, tracer));
  }

  @Override
  public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
    delegate.addCallback(new TracedListenableFutureCallback<>(successCallback, failureCallback, tracer, span));
  }

  @Override
  public CompletableFuture<T> completable() {
    return delegate.completable();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return delegate.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return delegate.isCancelled();
  }

  @Override
  public boolean isDone() {
    return delegate.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return delegate.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.get(timeout, unit);
  }
}

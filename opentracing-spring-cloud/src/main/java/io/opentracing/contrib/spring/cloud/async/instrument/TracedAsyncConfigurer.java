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
package io.opentracing.contrib.spring.cloud.async.instrument;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;

/**
 * @author kameshsampath
 */
public class TracedAsyncConfigurer extends AsyncConfigurerSupport {

  private final Tracer tracer;
  private final AsyncConfigurer delegate;

  public TracedAsyncConfigurer(Tracer tracer, AsyncConfigurer delegate) {
    this.tracer = tracer;
    this.delegate = delegate;
  }

  @Override
  public Executor getAsyncExecutor() {
    return new TracedExecutor(this.delegate.getAsyncExecutor(), this.tracer);
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return delegate.getAsyncUncaughtExceptionHandler();
  }
}

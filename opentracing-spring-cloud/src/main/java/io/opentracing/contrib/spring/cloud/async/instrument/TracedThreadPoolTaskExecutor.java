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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;

/**
 * @author kameshsampath
 */
public class TracedThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private final Tracer tracer;
    private final ThreadPoolTaskExecutor delegate;

    public TracedThreadPoolTaskExecutor(Tracer tracer, ThreadPoolTaskExecutor delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable task) {
        this.delegate.execute(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        this.delegate.execute(new TracedRunnable(task, activeSpan()), startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.delegate.submit(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.delegate.submit(new TracedCallable<>(task, activeSpan()));
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return this.delegate.submitListenable(new TracedRunnable(task, activeSpan()));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return this.delegate.submitListenable(new TracedCallable<>(task, activeSpan()));
    }

    @Override
    public void afterPropertiesSet() {
        this.delegate.afterPropertiesSet();
        super.afterPropertiesSet();
    }

    @Override
    public void destroy() {
        this.delegate.destroy();
        super.destroy();
    }

    @Override
    public void shutdown() {
        this.delegate.shutdown();
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        return this.delegate.getThreadPoolExecutor();
    }

    private ActiveSpan activeSpan() {
        return tracer.activeSpan();
    }
}

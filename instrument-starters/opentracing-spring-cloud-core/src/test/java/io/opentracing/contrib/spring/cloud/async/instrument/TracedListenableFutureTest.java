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
import io.opentracing.mock.MockTracer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Tests for {@link TracedListenableFuture}, to ensure context is propagated to all callbacks.
 *
 * @author MiNG
 */
public class TracedListenableFutureTest {

  private static final MockTracer TRACER = new MockTracer();
  private static final TracedThreadPoolTaskExecutor EXECUTOR;
  static {
    final ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
    delegate.initialize();
    EXECUTOR = new TracedThreadPoolTaskExecutor(TRACER, delegate);
  }
  private static final TracedThreadPoolTaskScheduler SCHEDULER;
  static {
    final ThreadPoolTaskScheduler delegate = new ThreadPoolTaskScheduler();
    delegate.initialize();
    SCHEDULER = new TracedThreadPoolTaskScheduler(TRACER, delegate);
  }

  @AfterClass
  public static void cleanup() {
    EXECUTOR.destroy();
    SCHEDULER.destroy();
    TRACER.close();
  }

  @Test
  public void executor_submitListenable_runnable_onSuccess() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_runnable_onSuccess").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = EXECUTOR.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> callbackResult.complete(span == TRACER.activeSpan()),
          e -> { /* NOOP */ }
      );
      listenableFuture.get();
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void executor_submitListenable_callable_onSuccess() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_callable_onSuccess").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = EXECUTOR.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        return null;
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> callbackResult.complete(span == TRACER.activeSpan()),
          e -> { /* NOOP */ }
      );
      listenableFuture.get();
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void executor_submitListenable_runnable_onFailure() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_runnable_onFailure").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = EXECUTOR.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        throw new Exception();
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> { /* NOOP */ },
          e -> {
            if (e instanceof AssertionError) {
              callbackResult.completeExceptionally(e);
            } else {
              callbackResult.complete(span == TRACER.activeSpan());
            }
          }
      );
      try {
        listenableFuture.get();
        Assert.fail();
      } catch (Exception e) {
        // success
      }
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void executor_submitListenable_callable_onFailure() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_callable_onFailure").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = EXECUTOR.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        throw new Exception();
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> { /* NOOP */ },
          e -> {
            if (e instanceof AssertionError) {
              callbackResult.completeExceptionally(e);
            } else {
              callbackResult.complete(span == TRACER.activeSpan());
            }
          }
      );
      try {
        listenableFuture.get();
        Assert.fail();
      } catch (Exception e) {
        // success
      }
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void scheduler_submitListenable_runnable_onSuccess() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_runnable_onSuccess").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = SCHEDULER.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> callbackResult.complete(span == TRACER.activeSpan()),
          e -> { /* NOOP */ }
      );
      listenableFuture.get();
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void scheduler_submitListenable_callable_onSuccess() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_callable_onSuccess").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = SCHEDULER.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        return null;
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> callbackResult.complete(span == TRACER.activeSpan()),
          e -> { /* NOOP */ }
      );
      listenableFuture.get();
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void scheduler_submitListenable_runnable_onFailure() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_runnable_onFailure").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = SCHEDULER.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        throw new Exception();
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> { /* NOOP */ },
          e -> {
            if (e instanceof AssertionError) {
              callbackResult.completeExceptionally(e);
            } else {
              callbackResult.complete(span == TRACER.activeSpan());
            }
          }
      );
      try {
        listenableFuture.get();
        Assert.fail();
      } catch (Exception e) {
        // success
      }
      Assert.assertTrue(callbackResult.get());
    }
  }

  @Test
  public void scheduler_submitListenable_callable_onFailure() throws Exception {
    final Span span = TRACER.buildSpan("executor_submitListenable_callable_onFailure").start();
    try (Scope ignored = TRACER.activateSpan(span)) {
      final ListenableFuture<?> listenableFuture = SCHEDULER.submitListenable(() -> {
        // Force callback run in thread pool
        LockSupport.parkNanos(100_000_000);
        Assert.assertSame(span, TRACER.activeSpan());
        throw new Exception();
      });
      final CompletableFuture<Boolean> callbackResult = new CompletableFuture<>();
      listenableFuture.addCallback(
          r -> { /* NOOP */ },
          e -> {
            if (e instanceof AssertionError) {
              callbackResult.completeExceptionally(e);
            } else {
              callbackResult.complete(span == TRACER.activeSpan());
            }
          }
      );
      try {
        listenableFuture.get();
        Assert.fail();
      } catch (Exception e) {
        // success
      }
      Assert.assertTrue(callbackResult.get());
    }
  }
}

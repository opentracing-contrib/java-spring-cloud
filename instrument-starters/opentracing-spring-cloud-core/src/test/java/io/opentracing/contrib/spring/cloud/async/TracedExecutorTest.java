/*
 * Copyright 2017-2021 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.async;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(classes = {MockTracingConfiguration.class, TracedExecutorTest.Configuration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracedExecutorTest {

  @org.springframework.context.annotation.Configuration
  static class Configuration {

    @Bean
    public Executor threadPoolTaskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.initialize();
      return executor;
    }

    @Bean
    public Executor simpleAsyncTaskExecutor() {
      return new SimpleAsyncTaskExecutor();
    }

    @Bean
    public Executor runtimeExceptionThrowingExecutor() {
      return command -> {
        throw new RejectedExecutionException("Some runtime exception thrown by executor.");
      };
    }

    @Bean
    public ExecutorService executorService() {
      return Executors.newFixedThreadPool(1);
    }
  }

  @Autowired
  private MockTracer mockTracer;
  @Qualifier("threadPoolTaskExecutor")
  @Autowired
  private Executor threadPoolExecutor;
  @Qualifier("simpleAsyncTaskExecutor")
  @Autowired
  private Executor simpleAsyncExecutor;
  @Qualifier("runtimeExceptionThrowingExecutor")
  @Autowired
  private Executor runtimeExceptionThrowingExecutor;

  @Qualifier("executorService")
  @Autowired
  private ExecutorService executorService;


  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testThreadPoolTracedExecutor() {
    testTracedExecutor(threadPoolExecutor);
  }

  @Test
  public void testSimpleTracedExecutor() {
    testTracedExecutor(simpleAsyncExecutor);
  }

  private void testTracedExecutor(Executor executor) {
    Span span = mockTracer.buildSpan("foo").start();
    try (Scope scope = mockTracer.activateSpan(span)) {
      CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
        mockTracer.buildSpan("child").start().finish();
        return "ok";
      }, executor);
      completableFuture.join();
    }
    span.finish();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }

  @Test
  public void testExceptionThrownByExecutorShouldBeRethrownByTracedExecutor() {
    try {
      CompletableFuture.runAsync(
          () -> { },
          runtimeExceptionThrowingExecutor
      );
      fail();
    } catch (Exception ex) {
      assertEquals(RejectedExecutionException.class, ex.getClass());
      assertEquals(ex.getMessage(), "Some runtime exception thrown by executor.");
    }
  }

  @Test
  public void testExecutorService() throws Exception {
    Span span = mockTracer.buildSpan("foo").start();
    try (Scope scope = mockTracer.activateSpan(span)) {
      Future<?> child = executorService.submit(() -> mockTracer.buildSpan("child").start().finish());
      child.get();
    }
    span.finish();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }
}

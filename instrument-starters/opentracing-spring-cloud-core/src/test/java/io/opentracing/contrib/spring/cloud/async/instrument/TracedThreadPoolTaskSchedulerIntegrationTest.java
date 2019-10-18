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
package io.opentracing.contrib.spring.cloud.async.instrument;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.time.Instant;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author cbono
 */
@SpringBootTest(classes = {
    MockTracingConfiguration.class,
    TracedThreadPoolTaskSchedulerIntegrationTest.TestConfiguration.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracedThreadPoolTaskSchedulerIntegrationTest {

  @Configuration
  static class TestConfiguration {

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
      final ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
      executor.initialize();
      return executor;
    }
  }

  @Autowired
  private MockTracer mockTracer;

  @Autowired
  @Qualifier("threadPoolTaskScheduler")
  private ThreadPoolTaskScheduler threadPoolTaskScheduler;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testExecute() {
    final Span span = mockTracer.buildSpan("5150").start();
    try (Scope scope = mockTracer.activateSpan(span)) {
      final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
        mockTracer.buildSpan("child").start().finish();
        return "ok";
      }, threadPoolTaskScheduler);
      completableFuture.join();
    }
    span.finish();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    final List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }

  @Test
  public void testSumbit() throws Exception {
    final Span span = mockTracer.buildSpan("5150").start();
    try (Scope scope = mockTracer.activateSpan(span)) {
      final Future<?> child = threadPoolTaskScheduler.submit(() -> mockTracer.buildSpan("child").start().finish());
      child.get();
    }
    span.finish();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    final List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }

  @Test
  public void testSchedule() throws Exception {
    final Span span = mockTracer.buildSpan("5150").start();
    try (Scope scope = mockTracer.activateSpan(span)) {
      final Future<?> child = threadPoolTaskScheduler.schedule(
          () -> mockTracer.buildSpan("child").start().finish(),
          Instant.now().plusSeconds(5));
      child.get();
    }
    span.finish();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    final List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }
}

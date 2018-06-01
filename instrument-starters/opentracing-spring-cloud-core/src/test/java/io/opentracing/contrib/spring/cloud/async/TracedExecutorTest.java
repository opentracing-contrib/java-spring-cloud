/**
 * Copyright 2017-2018 The OpenTracing Authors
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

import io.opentracing.Scope;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
  }

  @Autowired
  private MockTracer mockTracer;
  @Qualifier("threadPoolTaskExecutor")
  @Autowired
  private Executor threadPoolExecutor;
  @Qualifier("simpleAsyncTaskExecutor")
  @Autowired
  private Executor simpleAsyncExecutor;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testThreadPoolTracedExecutor() throws ExecutionException, InterruptedException {
    testTracedExecutor(threadPoolExecutor);
  }

  @Test
  public void testSimpleTracedExecutor() throws ExecutionException, InterruptedException {
    testTracedExecutor(simpleAsyncExecutor);
  }

  private void testTracedExecutor(Executor executor) {
    try (Scope scope = mockTracer.buildSpan("foo").startActive(true)) {
      CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
        mockTracer.buildSpan("child").start().finish();
        return "ok";
      }, executor);
      completableFuture.join();
    }
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }
}

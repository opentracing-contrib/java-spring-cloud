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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.Scope;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author kameshs
 */
@SpringBootTest(classes = {MockTracingConfiguration.class, AsyncAnnotationTest.Configuration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncAnnotationTest {

  @org.springframework.context.annotation.Configuration
  static class Configuration {

    @Bean
    public AsyncService delayAsyncService() {
      return new AsyncService();
    }
  }

  static class AsyncService {

    @Autowired
    private MockTracer tracer;

    @Async
    public Future<String> fooAsync() {
      tracer.buildSpan("foo").start().finish();
      return new AsyncResult<>("whatever");
    }

    @Async
    public Future<String> fooException() {
      throw new RuntimeException();
    }

  }

  @Autowired
  private MockTracer mockTracer;
  @Autowired
  private AsyncService asyncService;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testAsyncTraceAndSpans() throws Exception {
    try (Scope scope = mockTracer.buildSpan("bar")
        .startActive(true)) {
      Future<String> fut = asyncService.fooAsync();
      await().until(() -> fut.isDone());
      assertThat(fut.get()).isNotNull();
    }
    await().until(() -> mockTracer.finishedSpans().size() == 3);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    // parent span from test, span modelling @Async, span inside @Async
    assertEquals(3, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
    MockSpan asyncSpan = mockSpans.get(1);
    assertEquals(3, asyncSpan.tags().size());
    assertEquals(TraceAsyncAspect.TAG_COMPONENT, asyncSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals("fooAsync", asyncSpan.tags().get(ExtensionTags.METHOD_TAG.getKey()));
    assertEquals(AsyncService.class.getSimpleName(),
        asyncSpan.tags().get(ExtensionTags.CLASS_TAG.getKey()));
  }

  @Test
  public void testExceptionThrown() {
    try {
      asyncService.fooException();
    } catch (Exception e) {
      e.printStackTrace();
    }
    await().until(() -> mockTracer.finishedSpans().size() == 1);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    assertEquals(4, mockSpans.get(0).tags().size());
    assertEquals(Boolean.TRUE, mockSpans.get(0).tags().get(Tags.ERROR.getKey()));
    assertEquals(1, mockSpans.get(0).logEntries().size());
    assertEquals("error", mockSpans.get(0).logEntries().get(0).fields().get("event"));
    assertTrue(mockSpans.get(0).logEntries().get(0).fields()
        .get("error.object") instanceof RuntimeException);
  }
}

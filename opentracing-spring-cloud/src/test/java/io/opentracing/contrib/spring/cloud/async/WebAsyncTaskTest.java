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
package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(classes = {MockTracingConfiguration.class,
    WebAsyncTaskTest.HelloWorldController.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
public class WebAsyncTaskTest {

  @RestController
  static class HelloWorldController {

    @Autowired
    MockTracer mockTracer;

    @RequestMapping("/webAsyncTask")
    public WebAsyncTask<String> webAsyncTask() {
      return new WebAsyncTask<>(() -> {
        mockTracer.buildSpan("foo").startManual().finish();
        return "webAsyncTask";
      });
    }

    @RequestMapping("/callable")
    public Callable<String> callable() {
      return () -> {
        mockTracer.buildSpan("foo").startManual().finish();
        return "callable";
      };
    }
  }

  @Autowired
  private MockTracer mockTracer;
  @Autowired
  private TestRestTemplate restTemplate;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testWebAsyncTaskTraceAndSpans() throws Exception {
    String response = restTemplate.getForObject("/webAsyncTask", String.class);
    assertThat(response).isNotNull();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }

  @Test
  public void testCallableTraceAndSpans() throws Exception {
    String response = restTemplate.getForObject("/callable", String.class);
    assertThat(response).isNotNull();
    await().until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
  }
}

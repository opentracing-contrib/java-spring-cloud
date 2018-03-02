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
package io.opentracing.contrib.spring.cloud.web;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestController;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class},
    properties = {"opentracing.spring.web.skipPattern=/notTraced"})
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringWebTracingTest {

  @LocalServerPort
  int port;

  @Autowired
  protected MockTracer mockTracer;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private AsyncRestTemplate asyncRestTemplate;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testControllerTracing() {
    ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/hello", String.class);
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    assertEquals(200, responseEntity.getStatusCode().value());
    assertEquals(1, mockTracer.finishedSpans().size());
  }

  @Test
  public void testRestTemplateTracing() {
    restTemplate.getForEntity(getUrl("/notTraced"), String.class);
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    //only http client is traced
    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
  }

  @Test
  public void testAsyncRestTemplateTracing() throws ExecutionException, InterruptedException {
    asyncRestTemplate.getForEntity(getUrl("/notTraced"), String.class).get();
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
  }

  public String getUrl(String path) {
    return "http://localhost:" + port + path;
  }
}

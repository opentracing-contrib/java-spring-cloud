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
package io.opentracing.contrib.spring.cloud.zuul;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestController;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {MockTracingConfiguration.class, ZuulTracingTest.ZuulConfiguration.class,
        TestController.class},
    properties = {"zuul.routes.test.url=http://localhost:53751/hello",
        "zuul.routes.wrong.url=http://localhost:53751/it-doesnt-exist",
        "zuul.routes.wrong-port.url=http://localhost:53752/wrong-port",
        "server.port=53751"})
@RunWith(SpringJUnit4ClassRunner.class)
public class ZuulTracingTest {

  @Autowired
  protected MockTracer mockTracer;

  @Autowired
  private TestRestTemplate restTemplate;

  @Configuration
  @EnableZuulProxy
  static class ZuulConfiguration {

  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testZuulTracing() {
    restTemplate.getForEntity("/test", String.class);

    await().until(() -> mockTracer.finishedSpans().size() == 3);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(3, mockSpans.size());

    // 2 spans from java-web-servlet, 1 from zuul
    assertEquals(2, getSpansByComponentName(mockSpans, "java-web-servlet").size());

    MockSpan zuulSpan = getSpansByComponentName(mockSpans, TracePreZuulFilter.COMPONENT_NAME)
        .get(0);
    assertEquals(200, zuulSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    assertEquals("http://localhost:53751/hello",
        zuulSpan.tags().get(TracePostZuulFilter.ROUTE_HOST_TAG));
    assertEquals("GET", zuulSpan.operationName());
    assertEquals(0, zuulSpan.generatedErrors().size());

    TestUtils.assertSameTraceId(mockSpans);
  }

  @Test
  public void testWrongZuulRoute() {
    restTemplate.getForEntity("/wrong", String.class);

    // 2 spans from servlet filter, 1 from zuul, 1 from error handler
    await().until(() -> mockTracer.finishedSpans().size() == 4);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(4, mockSpans.size());

    MockSpan zuulSpan = getSpansByComponentName(mockSpans, TracePreZuulFilter.COMPONENT_NAME)
        .get(0);
    assertEquals(TracePreZuulFilter.COMPONENT_NAME, zuulSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals(404, zuulSpan.tags().get(Tags.HTTP_STATUS.getKey()));
  }

  @Test
  public void testWrongZuulRoutePort() {
    restTemplate.getForEntity("/wrong-port", String.class);

    // 1 span from servlet filter, 1 from zuul
    await().until(() -> mockTracer.finishedSpans().size() == 2);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());

    MockSpan zuulSpan = getSpansByComponentName(mockSpans, TracePreZuulFilter.COMPONENT_NAME)
        .get(0);
    assertEquals(TracePreZuulFilter.COMPONENT_NAME, zuulSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals(500, zuulSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    assertEquals(1, zuulSpan.logEntries().size());
    assertEquals(Tags.ERROR.getKey(), zuulSpan.logEntries().get(0).fields().get("event"));
    assertTrue(zuulSpan.logEntries().get(0).fields().containsKey("error.object"));
  }

  @Test
  public void testNoZuulTracing() {
    restTemplate.getForEntity("/hello", String.class);

    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    // span from java-web-servlet component
    assertEquals("java-web-servlet", mockSpans.get(0).tags().get(Tags.COMPONENT.getKey()));
  }

  public List<MockSpan> getSpansByComponentName(List<MockSpan> spans, String componentName) {
    return spans.stream().filter(mockSpan ->
        componentName.equals(mockSpan.tags().get(Tags.COMPONENT.getKey())))
        .collect(Collectors.toList());
  }
}

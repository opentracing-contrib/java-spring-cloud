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
package io.opentracing.contrib.spring.cloud.feign;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import com.netflix.hystrix.strategy.HystrixPlugins;
import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Pavol Loffay
 */
@TestPropertySource(properties = {
    "feign.hystrix.enabled=true",
    "opentracing.spring.web.skipPattern=/notTraced"})
public class FeignHystrixTest extends FeignTest {

  @BeforeClass
  public static void beforeClass() {
    //reset because concurrent strategy is registered with different tracer
    HystrixPlugins.reset();
  }

  @Test
  public void testParentSpanRequest() throws InterruptedException {
    // create parent span to verify that Hystrix is instrumented and it propagates spans to callables
    MockSpan parentSpan = mockTracer.buildSpan("parent").startManual();
    try (Scope scope = mockTracer.scopeManager().activate(parentSpan, true)) {
      feignInterface.hello();
      await().until(() -> mockTracer.finishedSpans().size() == 1);
      List<MockSpan> mockSpans = mockTracer.finishedSpans();
      assertEquals(1, mockSpans.size());
      verify(mockTracer);
      assertEquals(parentSpan.context().spanId(), mockSpans.get(0).parentId());
    }
  }
}

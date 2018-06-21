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
import static org.junit.Assert.assertNull;

import io.opentracing.contrib.spring.cloud.feign.BaseFeignTest.FeignRibbonLocalConfiguration;
import io.opentracing.contrib.spring.cloud.feign.FeignSpanDecoratorConfiguration.AnotherFeignSpanDecorator;
import io.opentracing.contrib.spring.cloud.feign.FeignSpanDecoratorConfiguration.MyFeignSpanDecorator;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class,
        FeignRibbonLocalConfiguration.class},
    properties = {"opentracing.spring.web.skipPattern=/notTraced"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignWithoutSpanDecoratorsTest extends BaseFeignTest {

  @Test
  public void testTracedRequest() {
    feignInterface.hello();
    verifyMockTracer(mockTracer);
  }

  private void verifyMockTracer(MockTracer mockTracer) {
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    Map<String, Object> tags = mockSpans.get(0).tags();
    assertEquals(Tags.SPAN_KIND_CLIENT, tags.get(Tags.SPAN_KIND.getKey()));
    assertNull(tags.get(MyFeignSpanDecorator.TAG_NAME));
    assertNull(tags.get(AnotherFeignSpanDecorator.TAG_NAME));
  }
}

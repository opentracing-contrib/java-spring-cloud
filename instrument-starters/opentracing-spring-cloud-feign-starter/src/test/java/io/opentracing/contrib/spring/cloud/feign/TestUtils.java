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

import io.opentracing.contrib.spring.cloud.feign.FeignSpanDecoratorConfiguration.AnotherFeignSpanDecorator;
import io.opentracing.contrib.spring.cloud.feign.FeignSpanDecoratorConfiguration.MyFeignSpanDecorator;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.Map;

/**
 * @author Emerson Oliveira
 */
public class TestUtils {

  static void verifyWithSpanDecorators(MockTracer mockTracer) {
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    Map<String, Object> tags = mockSpans.get(0).tags();
    assertEquals(Tags.SPAN_KIND_CLIENT, tags.get(Tags.SPAN_KIND.getKey()));
    assertEquals(MyFeignSpanDecorator.TAG_VALUE, tags.get(MyFeignSpanDecorator.TAG_NAME));
    assertEquals(AnotherFeignSpanDecorator.TAG_VALUE, tags.get(AnotherFeignSpanDecorator.TAG_NAME));
  }

}

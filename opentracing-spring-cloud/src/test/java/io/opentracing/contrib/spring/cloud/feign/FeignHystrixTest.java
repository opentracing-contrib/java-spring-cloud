package io.opentracing.contrib.spring.cloud.feign;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
import java.util.List;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Pavol Loffay
 */
@TestPropertySource(properties = {"feign.hystrix.enabled=true"})
public class FeignHystrixTest extends FeignTest {

  @Test
  public void testParentSpanRequest() throws InterruptedException {
    // create parent span to verify that Hystrix is instrumented and it propagates spans to callables
    MockSpan parentSpan = mockTracer.buildSpan("parent").startManual();
    try (ActiveSpan activeSpan = mockTracer.makeActive(parentSpan)) {
      feignInterface.hello();
      await().until(() -> mockTracer.finishedSpans().size() == 2);
      List<MockSpan> mockSpans = mockTracer.finishedSpans();
      assertEquals(2, mockSpans.size());
      verify(mockTracer);
      assertEquals(parentSpan.context().spanId(), toComponentMap(mockSpans).get(Tags.SPAN_KIND_CLIENT).parentId());
    }
  }
}

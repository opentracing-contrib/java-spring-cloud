package io.opentracing.contrib.spring.cloud;

import java.util.Collection;

import io.opentracing.mock.MockSpan;
import org.junit.Assert;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TestUtils {
  /**
   * Check if all spans have same traceId.
   *
   * @param spans the spans to check
   */
  public static void assertSameTraceId(Collection<MockSpan> spans) {
    if (!spans.isEmpty()) {
      final long traceId = spans.iterator().next().context().traceId();
      for (MockSpan span : spans) {
        Assert.assertEquals(traceId, span.context().traceId());
      }
    }
  }
}

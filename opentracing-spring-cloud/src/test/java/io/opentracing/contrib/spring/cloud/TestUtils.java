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
package io.opentracing.contrib.spring.cloud;

import io.opentracing.mock.MockSpan;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

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

  public static <T> void invokePrivateConstructor(Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      constructor.newInstance();
    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      throw new RuntimeException("Error invoking private constructor!", e); // NOPMD
    }
  }

  public static void verifyNoMoreInteractionsWithMocks(Object testClass) {
    Object[] annotatedMocks = getObjectsAnnotatedWithMock(testClass);
    if (annotatedMocks.length > 0) {
      Mockito.verifyNoMoreInteractions(annotatedMocks);
    }
  }

  private static Object[] getObjectsAnnotatedWithMock(final Object testClass) {
    return Arrays.stream(testClass.getClass().getDeclaredFields())
        .filter(input -> input.isAnnotationPresent(Mock.class))
        .map(
            input -> {
              ReflectionUtils.makeAccessible(input);
              return ReflectionUtils.getField(input, testClass);
            })
        .toArray();
  }
}

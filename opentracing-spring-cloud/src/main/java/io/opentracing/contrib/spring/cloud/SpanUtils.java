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

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Pavol Loffay
 */
public class SpanUtils {

  private SpanUtils() {
  }

  /**
   * Add appropriate error tags and logs to a span when an exception occurs
   *
   * @param span the span "monitoring" the code which threw the exception
   * @param ex captured exception
   */
  public static void captureException(Span span, Exception ex) {
    Map<String, Object> exceptionLogs = new LinkedHashMap<>(2);
    exceptionLogs.put("event", Tags.ERROR.getKey());
    exceptionLogs.put("error.object", ex);
    span.log(exceptionLogs);
    Tags.ERROR.set(span, true);
  }
}

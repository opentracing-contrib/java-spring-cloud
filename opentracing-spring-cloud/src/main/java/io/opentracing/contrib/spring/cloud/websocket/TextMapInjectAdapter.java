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
package io.opentracing.contrib.spring.cloud.websocket;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

/**
 * A TextMap carrier for use with Tracer.inject() to inject tracing context into Spring messaging
 * {@link MessageHeaders}.
 *
 * @see Tracer#inject(SpanContext, Format, Object)
 */
public class TextMapInjectAdapter implements TextMap {

  private final MessageBuilder<?> messageBuilder;

  public TextMapInjectAdapter(MessageBuilder<?> messageBuilder) {
    this.messageBuilder = messageBuilder;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    throw new UnsupportedOperationException(
        TextMapInjectAdapter.class.getName() + " should only be used with Tracer.inject()");
  }

  @Override
  public void put(String key, String value) {
    messageBuilder.setHeader(key, value);
  }

}

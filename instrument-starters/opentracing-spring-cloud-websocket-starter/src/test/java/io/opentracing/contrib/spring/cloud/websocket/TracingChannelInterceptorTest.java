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
package io.opentracing.contrib.spring.cloud.websocket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

public class TracingChannelInterceptorTest {

  private static final String TEST_DESTINATION = "/app/test";

  private MockTracer mockTracer = new MockTracer();

  @Test
  public void testPreSendServerSpan() {
    MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("Hi")
        .setHeader(TracingChannelInterceptor.SIMP_MESSAGE_TYPE, SimpMessageType.MESSAGE)
        .setHeader(TracingChannelInterceptor.SIMP_DESTINATION, TEST_DESTINATION);

    MockSpan parentSpan = mockTracer.buildSpan("parent").startManual();
    mockTracer.inject(parentSpan.context(), Format.Builtin.TEXT_MAP,
        new TextMapInjectAdapter(messageBuilder));

    TracingChannelInterceptor interceptor = new TracingChannelInterceptor(mockTracer,
        Tags.SPAN_KIND_SERVER);

    Message<?> processed = interceptor.preSend(messageBuilder.build(), null);

    // Verify span cached with message is child of propagated parentSpan span context
    assertTrue(processed.getHeaders().containsKey(TracingChannelInterceptor.OPENTRACING_SPAN));
    MockSpan childSpan = (MockSpan) processed.getHeaders()
        .get(TracingChannelInterceptor.OPENTRACING_SPAN);
    assertEquals(parentSpan.context().spanId(), childSpan.parentId());
    assertEquals(parentSpan.context().traceId(), childSpan.context().traceId());
    assertEquals(TEST_DESTINATION, childSpan.operationName());
    assertEquals(Tags.SPAN_KIND_SERVER, childSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals(TracingChannelInterceptor.WEBSOCKET,
        childSpan.tags().get(Tags.COMPONENT.getKey()));
  }

  @Test
  public void testPreSendClientSpan() {
    MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("Hi")
        .setHeader(TracingChannelInterceptor.SIMP_MESSAGE_TYPE, SimpMessageType.MESSAGE)
        .setHeader(TracingChannelInterceptor.SIMP_DESTINATION, TEST_DESTINATION);

    MockSpan parentSpan = mockTracer.buildSpan("parent").startManual();
    Scope parentScope = mockTracer.scopeManager().activate(parentSpan, true);

    TracingChannelInterceptor interceptor = new TracingChannelInterceptor(mockTracer,
        Tags.SPAN_KIND_CLIENT);

    Message<?> processed = interceptor.preSend(messageBuilder.build(), null);

    parentScope.close();

    // Verify span cached with message is child of the active parentSpan
    assertTrue(processed.getHeaders().containsKey(TracingChannelInterceptor.OPENTRACING_SPAN));
    MockSpan childSpan = (MockSpan) processed.getHeaders()
        .get(TracingChannelInterceptor.OPENTRACING_SPAN);
    assertEquals(parentSpan.context().spanId(), childSpan.parentId());
    assertEquals(parentSpan.context().traceId(), childSpan.context().traceId());

    // Verify child span context propagated with message
    MockSpan.MockContext context = (MockSpan.MockContext)
        mockTracer
            .extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(processed.getHeaders()));
    assertEquals(childSpan.context().traceId(), context.traceId());
    assertEquals(childSpan.context().spanId(), context.spanId());
    assertEquals(TEST_DESTINATION, childSpan.operationName());
    assertEquals(Tags.SPAN_KIND_CLIENT, childSpan.tags().get(Tags.SPAN_KIND.getKey()));
    assertEquals(TracingChannelInterceptor.WEBSOCKET,
        childSpan.tags().get(Tags.COMPONENT.getKey()));
  }

}

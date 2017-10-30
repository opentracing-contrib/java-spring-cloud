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

package io.opentracing.contrib.spring.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentracing.ActiveSpan;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenTracingChannelInterceptorTest {

  @Mock
  private Tracer mockTracer;

  @Mock
  private Tracer.SpanBuilder mockSpanBuilder;

  @Mock
  private ActiveSpan mockActiveSpan;

  @Mock
  private SpanContext mockSpanContext;

  @Mock
  private MessageChannel mockMessageChannel;

  @Mock
  private AbstractMessageChannel mockAbstractMessageChannel;

  private Message<String> simpleMessage;

  private OpenTracingChannelInterceptor interceptor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.asChildOf(any(SpanContext.class))).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.startActive()).thenReturn(mockActiveSpan);
    when(mockActiveSpan.context()).thenReturn(mockSpanContext);

    interceptor = new OpenTracingChannelInterceptor(mockTracer);
    simpleMessage = MessageBuilder.withPayload("test")
        .build();
  }

  @Test
  public void preSendShouldGetNameFromGenericChannel() {
    interceptor.preSend(simpleMessage, mockMessageChannel);
    verify(mockTracer).buildSpan(String.format("message:%s", mockMessageChannel.toString()));
  }

  @Test
  public void preSendShouldGetNameFromAbstractMessageChannel() {
    interceptor.preSend(simpleMessage, mockAbstractMessageChannel);
    verify(mockAbstractMessageChannel, times(2)).getFullChannelName();
  }

  @Test
  public void preSendShouldStartSpanForClientSentMessage() {
    Message<?> message = interceptor.preSend(simpleMessage, mockMessageChannel);
    assertThat(message.getPayload()).isEqualTo(simpleMessage.getPayload());
    assertThat(message.getHeaders()).containsKey(Headers.MESSAGE_SENT_FROM_CLIENT);

    verify(mockTracer).extract(eq(Format.Builtin.TEXT_MAP), any(MessageTextMap.class));
    verify(mockTracer).buildSpan(String.format("message:%s", mockMessageChannel.toString()));
    verify(mockSpanBuilder).asChildOf((SpanContext) null);
    verify(mockSpanBuilder).startActive();
    verify(mockActiveSpan).setTag(Tags.COMPONENT.getKey(), "spring-messaging");
    verify(mockActiveSpan).setTag(Tags.MESSAGE_BUS_DESTINATION.getKey(), mockMessageChannel.toString());
    verify(mockActiveSpan).setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_PRODUCER);
    verify(mockActiveSpan).log(Events.CLIENT_SEND);
  }

  @Test
  public void preSendShouldStartSpanForServerReceivedMessage() {
    Message<?> originalMessage = MessageBuilder.fromMessage(simpleMessage)
        .setHeader(Headers.MESSAGE_SENT_FROM_CLIENT, true)
        .build();
    Message<?> message = interceptor.preSend(originalMessage, mockMessageChannel);
    assertThat(message.getPayload()).isEqualTo(originalMessage.getPayload());

    verify(mockTracer).extract(eq(Format.Builtin.TEXT_MAP), any(MessageTextMap.class));
    verify(mockTracer).buildSpan(String.format("message:%s", mockMessageChannel.toString()));
    verify(mockSpanBuilder).asChildOf((SpanContext) null);
    verify(mockSpanBuilder).startActive();
    verify(mockActiveSpan).setTag(Tags.COMPONENT.getKey(), "spring-messaging");
    verify(mockActiveSpan).setTag(Tags.MESSAGE_BUS_DESTINATION.getKey(), mockMessageChannel.toString());
    verify(mockActiveSpan).setTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CONSUMER);
    verify(mockActiveSpan).log(Events.SERVER_RECEIVE);
  }

  @Test
  public void preSendShouldStartChildSpanFromCarrier() {
    when(mockTracer.extract(eq(Format.Builtin.TEXT_MAP), any(MessageTextMap.class))).thenReturn(mockSpanContext);

    interceptor.preSend(simpleMessage, mockMessageChannel);

    verify(mockSpanBuilder).asChildOf(mockSpanContext);
  }

  @Test
  public void afterSendCompletionShouldDoNothingWithoutSpan() {
    interceptor.afterSendCompletion(null, null, true, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyString());
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForServerSendMessage() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);
    when(mockActiveSpan.getBaggageItem(Events.SERVER_RECEIVE)).thenReturn(Events.SERVER_RECEIVE);

    interceptor.afterSendCompletion(null, null, true, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Events.SERVER_SEND);
    verify(mockActiveSpan, times(0)).log(anyMap());
    verify(mockActiveSpan).close();
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForClientSendMessage() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterSendCompletion(null, null, true, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Events.CLIENT_RECEIVE);
    verify(mockActiveSpan, times(0)).log(anyMap());
    verify(mockActiveSpan).close();
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForException() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterSendCompletion(null, null, true, new Exception("test"));

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Events.CLIENT_RECEIVE);
    verify(mockActiveSpan).log(Collections.singletonMap(Events.ERROR, "test"));
    verify(mockActiveSpan).close();
  }

  @Test
  public void beforeHandleShouldDoNothingWithoutSpan() {
    interceptor.beforeHandle(null, null, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyString());
  }

  @Test
  public void beforeHandleShouldLogEvent() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.beforeHandle(null, null, null);

    verify(mockActiveSpan).log(Events.SERVER_RECEIVE);
  }

  @Test
  public void afterMessageHandledShouldDoNothingWithoutSpan() {
    interceptor.afterMessageHandled(null, null, null, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan, times(0)).log(anyString());
    verify(mockActiveSpan, times(0)).log(anyMap());
  }

  @Test
  public void afterMessageHandledShouldLogEvent() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterMessageHandled(null, null, null, null);

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Events.SERVER_SEND);
    verify(mockActiveSpan, times(0)).log(anyMap());
  }

  @Test
  public void afterMessageHandledShouldLogEventAndException() {
    when(mockTracer.activeSpan()).thenReturn(mockActiveSpan);

    interceptor.afterMessageHandled(null, null, null, new Exception("test"));

    verify(mockTracer).activeSpan();
    verify(mockActiveSpan).log(Events.SERVER_SEND);
    verify(mockActiveSpan).log(Collections.singletonMap(Events.ERROR, "test"));
  }

}

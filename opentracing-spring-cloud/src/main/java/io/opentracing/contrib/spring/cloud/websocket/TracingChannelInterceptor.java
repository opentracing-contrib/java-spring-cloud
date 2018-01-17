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

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

/**
 * This class implements a {@link ExecutorChannelInterceptor} to instrument the websocket
 * communications using an OpenTracing Tracer.
 */
public class TracingChannelInterceptor extends ChannelInterceptorAdapter implements
    ExecutorChannelInterceptor {

  /**
   * The span component tag value.
   */
  protected static final String WEBSOCKET = "websocket";

  /**
   * The STOMP simple destination.
   */
  protected static final String SIMP_DESTINATION = "simpDestination";

  /**
   * The STOMP simple message type, values defined in enum {@link SimpMessageType}.
   */
  protected static final String SIMP_MESSAGE_TYPE = "simpMessageType";

  /**
   * Indicates that the destination is unknown.
   */
  private static final String UNKNOWN_DESTINATION = "Unknown";

  /**
   * Header name used to carry the current {@link Span} from the initial preSend phase to the
   * beforeHandle phase.
   */
  protected static final String OPENTRACING_SPAN = "opentracing.span";

  private Tracer tracer;
  private String spanKind;

  public TracingChannelInterceptor(Tracer tracer, String spanKind) {
    this.tracer = tracer;
    this.spanKind = spanKind;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    if (SimpMessageType.MESSAGE.equals(message.getHeaders().get(SIMP_MESSAGE_TYPE))) {
      if (Tags.SPAN_KIND_SERVER.equals(spanKind)) {
        return preSendServerSpan(message);
      } else if (Tags.SPAN_KIND_CLIENT.equals(spanKind)) {
        return preSendClientSpan(message);
      }
    }
    return message;
  }

  private Message<?> preSendClientSpan(Message<?> message) {
    Span span = tracer.buildSpan((String) message.getHeaders()
        .getOrDefault(SIMP_DESTINATION, UNKNOWN_DESTINATION))
        .withTag(Tags.SPAN_KIND.getKey(), spanKind)
        .withTag(Tags.COMPONENT.getKey(), WEBSOCKET)
        .start();
    MessageBuilder<?> messageBuilder = MessageBuilder.fromMessage(message)
        .setHeader(OPENTRACING_SPAN, span);
    tracer
        .inject(span.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(messageBuilder));
    return messageBuilder.build();
  }

  private Message<?> preSendServerSpan(Message<?> message) {
    Span span = tracer.buildSpan((String) message.getHeaders()
        .getOrDefault(SIMP_DESTINATION, UNKNOWN_DESTINATION))
        .asChildOf(tracer
            .extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(message.getHeaders())))
        .withTag(Tags.SPAN_KIND.getKey(), spanKind)
        .withTag(Tags.COMPONENT.getKey(), WEBSOCKET)
        .start();
    return MessageBuilder.fromMessage(message)
        .setHeader(OPENTRACING_SPAN, span)
        .build();
  }

  @Override
  public void afterMessageHandled(Message<?> message, MessageChannel channel,
      MessageHandler handler, Exception arg3) {
    if ((handler instanceof WebSocketAnnotationMethodMessageHandler ||
        handler instanceof SubProtocolWebSocketHandler) &&
        SimpMessageType.MESSAGE.equals(message.getHeaders().get(SIMP_MESSAGE_TYPE))) {
      tracer.scopeManager().active().close();
    }
  }

  @Override
  public Message<?> beforeHandle(Message<?> message, MessageChannel channel,
      MessageHandler handler) {
    if ((handler instanceof WebSocketAnnotationMethodMessageHandler ||
        handler instanceof SubProtocolWebSocketHandler) &&
        SimpMessageType.MESSAGE.equals(message.getHeaders().get(SIMP_MESSAGE_TYPE))) {
      tracer.scopeManager().activate(message.getHeaders().get(OPENTRACING_SPAN, Span.class), true);
    }
    return message;
  }

}
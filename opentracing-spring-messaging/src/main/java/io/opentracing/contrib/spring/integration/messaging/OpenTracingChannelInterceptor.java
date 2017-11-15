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

import io.opentracing.ActiveSpan;
import io.opentracing.BaseSpan;
import io.opentracing.References;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.util.ClassUtils;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenTracingChannelInterceptor extends ChannelInterceptorAdapter implements ExecutorChannelInterceptor {
  private static final Log log = LogFactory.getLog(OpenTracingChannelInterceptor.class);

  static final String COMPONENT_NAME = "spring-messaging";

  private final Tracer tracer;

  public OpenTracingChannelInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    log.trace("Processing message before sending it to the channel");
    boolean isConsumer = message.getHeaders().containsKey(Headers.MESSAGE_SENT_FROM_CLIENT);

    SpanBuilder spanBuilder = tracer.buildSpan(getOperationName(channel, isConsumer))
        .withTag(Tags.SPAN_KIND.getKey(), isConsumer ? Tags.SPAN_KIND_CONSUMER : Tags.SPAN_KIND_PRODUCER)
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
        .withTag(Tags.MESSAGE_BUS_DESTINATION.getKey(), getChannelName(channel));

    MessageTextMap<?> carrier = new MessageTextMap<>(message);
    SpanContext extractedContext = tracer.extract(Format.Builtin.TEXT_MAP, carrier);
    if (isConsumer) {
      spanBuilder.addReference(References.FOLLOWS_FROM, extractedContext);
    } else if (tracer.activeSpan() == null) {
      // it's a client but active span is null
      // This is a fallback we try to add extractedContext in case there is something
      spanBuilder.asChildOf(extractedContext);
    }

    ActiveSpan span = spanBuilder.startActive();

    if (isConsumer) {
      log.trace("Marking span with server received");
      span.log(Events.SERVER_RECEIVE);
      carrier.put(Headers.MESSAGE_CONSUMED, "true");
      // TODO maybe we should remove Headers.MESSAGE_SENT_FROM_CLIENT header here?
    } else {
      log.trace("Marking span with client send");
      span.log(Events.CLIENT_SEND);
      carrier.put(Headers.MESSAGE_SENT_FROM_CLIENT, "true");
    }

    tracer.inject(span.context(), Format.Builtin.TEXT_MAP, carrier);
    return carrier.getMessage();
  }

  @Override
  public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
    ActiveSpan activeSpan = tracer.activeSpan();

    log.trace(String.format("Completed sending and current span is %s", activeSpan));

    if (activeSpan == null) {
      return;
    }

    if (message.getHeaders().containsKey(Headers.MESSAGE_CONSUMED)) {
      log.trace("Marking span with server send");
      activeSpan.log(Events.SERVER_SEND);
    } else {
      log.debug("Marking span with client received");
      activeSpan.log(Events.CLIENT_RECEIVE);
    }

    handleException(ex, activeSpan);
    log.trace("Closing messaging span " + activeSpan);
    activeSpan.close();
    log.trace(String.format("Messaging span %s successfully closed", activeSpan));
  }

  @Override
  public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
    ActiveSpan activeSpan = tracer.activeSpan();

    log.trace(String.format("Continuing span %s before handling message", activeSpan));

    if (activeSpan != null) {
      log.trace("Marking span with server received");
      activeSpan.log(Events.SERVER_RECEIVE);
      log.trace(String.format("Span %s successfully continued", activeSpan));
    }

    return message;
  }

  @Override
  public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
    ActiveSpan activeSpan = tracer.activeSpan();

    log.trace(String.format("Continuing span %s after message handled", activeSpan));

    if (activeSpan == null) {
      return;
    }

    log.trace("Marking span with server send");
    activeSpan.log(Events.SERVER_SEND);

    handleException(ex, activeSpan);
  }

  /**
   *  Add exception related tags and logs to a span
   *
   * @param ex exception or null
   * @param span span
   */
  protected void handleException(Exception ex, BaseSpan<?> span) {
    if (ex != null) {
      Tags.ERROR.set(span, true);
      // TODO add exception logs
    }
  }

  protected String getChannelName(MessageChannel messageChannel) {
    String name = null;
    if (ClassUtils.isPresent("org.springframework.integration.context.IntegrationObjectSupport", null)) {
      if (messageChannel instanceof IntegrationObjectSupport) {
        name = ((IntegrationObjectSupport) messageChannel).getComponentName();
      }
      if (name == null && messageChannel instanceof AbstractMessageChannel) {
        name = ((AbstractMessageChannel) messageChannel).getFullChannelName();
      }
    }

    if (name == null) {
      return messageChannel.toString();
    }

    return name;
  }

  protected String getOperationName(MessageChannel messageChannel, boolean isConsumer) {
    String channelName = getChannelName(messageChannel);
    return String.format("%s:%s", isConsumer ? "receive" : "send", channelName);
  }
}

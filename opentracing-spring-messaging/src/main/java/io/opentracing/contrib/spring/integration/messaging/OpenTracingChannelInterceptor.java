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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.util.Collections;
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

  private static final String COMPONENT_TAG = "spring-messaging";

  private static final String MESSAGE_COMPONENT = "message";

  private final Tracer tracer;

  public OpenTracingChannelInterceptor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    log.trace("Processing message before sending it to the channel");

    MessageTextMap<?> carrier = new MessageTextMap<>(message);
    SpanContext parentSpan = tracer.extract(Format.Builtin.TEXT_MAP, carrier);
    String operationName = getOperationName(channel);
    ActiveSpan span = tracer.buildSpan(operationName)
        .asChildOf(parentSpan)
        .startActive();

    Tags.COMPONENT.set(span, COMPONENT_TAG);
    Tags.MESSAGE_BUS_DESTINATION.set(span, getChannelName(channel));

    if (message.getHeaders()
        .containsKey(Headers.MESSAGE_SENT_FROM_CLIENT)) {
      log.trace("Marking span with server received");
      span.log(Events.SERVER_RECEIVE);
      span.setBaggageItem(Events.SERVER_RECEIVE, Events.SERVER_RECEIVE);
      Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CONSUMER);
      // TODO maybe we should remove Headers.MESSAGE_SENT_FROM_CLIENT header here?
    } else {
      log.trace("Marking span with client send");
      span.log(Events.CLIENT_SEND);
      Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_PRODUCER);
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

    if (activeSpan.getBaggageItem(Events.SERVER_RECEIVE) != null) {
      log.trace("Marking span with server send");
      activeSpan.log(Events.SERVER_SEND);
    } else {
      log.debug("Marking span with client received");
      activeSpan.log(Events.CLIENT_RECEIVE);
    }

    if (ex != null) {
      activeSpan.log(Collections.singletonMap(Events.ERROR, ex.getMessage()));
    }

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

    if (ex != null) {
      activeSpan.log(Collections.singletonMap(Events.ERROR, ex.getMessage()));
    }
  }

  private String getChannelName(MessageChannel messageChannel) {
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

  private String getOperationName(MessageChannel messageChannel) {
    String channelName = getChannelName(messageChannel);

    return String.format("%s:%s", MESSAGE_COMPONENT, channelName);
  }

}

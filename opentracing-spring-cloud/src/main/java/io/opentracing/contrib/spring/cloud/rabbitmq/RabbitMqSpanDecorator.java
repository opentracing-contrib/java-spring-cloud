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
package io.opentracing.contrib.spring.cloud.rabbitmq;

import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.SpanUtils;
import io.opentracing.tag.Tags;

import org.springframework.amqp.core.MessageProperties;

/**
 * @author Gilles Robert
 */
interface RabbitMqSpanDecorator {

  void onSend(MessageProperties messageProperties, String exchange, String routingKey, Span span);

  void onReceive(MessageProperties messageProperties, Span span);

  void onError(Exception ex, Span span);

  class StandardTags implements RabbitMqSpanDecorator {

    @Override
    public void onSend(
        MessageProperties messageProperties, String exchange, String routingKey, Span span) {
      Tags.COMPONENT.set(span, RabbitMqTracingTags.MESSAGE_PRODUCER);
      RabbitMqTracingTags.EXCHANGE.set(span, exchange);
      RabbitMqTracingTags.MESSAGE_ID.set(span, messageProperties.getMessageId());
      RabbitMqTracingTags.ROUTING_KEY.set(span, routingKey);
    }

    @Override
    public void onReceive(MessageProperties messageProperties, Span span) {
      Tags.COMPONENT.set(span, RabbitMqTracingTags.MESSAGE_LISTENER);
      RabbitMqTracingTags.EXCHANGE.set(span, messageProperties.getReceivedExchange());
      RabbitMqTracingTags.MESSAGE_ID.set(span, messageProperties.getMessageId());
      RabbitMqTracingTags.ROUTING_KEY.set(span, messageProperties.getReceivedRoutingKey());
      RabbitMqTracingTags.CONSUMER_QUEUE.set(span, messageProperties.getConsumerQueue());
    }

    @Override
    public void onError(Exception ex, Span span) {
      SpanUtils.captureException(span, ex);
    }
  }
}

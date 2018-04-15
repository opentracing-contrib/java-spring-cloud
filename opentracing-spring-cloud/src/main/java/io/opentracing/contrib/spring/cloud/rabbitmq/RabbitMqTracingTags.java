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

import io.opentracing.tag.StringTag;

/**
 * @author Gilles Robert
 */
final class RabbitMqTracingTags {

  private static final String RABBITMQ = "rabbitMQ";

  static final String MESSAGE_PRODUCER = "message-producer";
  static final String MESSAGE_LISTENER = "message-listener";
  static final StringTag MESSAGE_ID = new StringTag("messageId");
  static final StringTag ROUTING_KEY = new StringTag("routingKey");
  static final StringTag CONSUMER_QUEUE = new StringTag("consumerQueue");
  static final StringTag EXCHANGE = new StringTag("exchange");
  static final String SPAN_KIND_PRODUCER = RABBITMQ + "-send";
  static final String SPAN_KIND_CONSUMER = RABBITMQ + "-receive";

  private RabbitMqTracingTags() {}
}

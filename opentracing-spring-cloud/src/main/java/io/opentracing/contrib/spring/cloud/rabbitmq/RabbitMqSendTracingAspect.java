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

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * @author Gilles Robert
 */
@Aspect
class RabbitMqSendTracingAspect {

  private final Tracer tracer;
  private final MessageConverter messageConverter;

  RabbitMqSendTracingAspect(Tracer tracer, MessageConverter messageConverter) {
    this.tracer = tracer;
    this.messageConverter = messageConverter;
  }

  // CHECKSTYLE:OFF
  @Around(value = "execution(* org.springframework.amqp.core.AmqpTemplate.convertAndSend(..)) && args(exchange,"
            + "routingKey, message)", argNames = "pjp,exchange,routingKey,message"
  )
  public Object traceRabbitSend(
      ProceedingJoinPoint pjp,
      String exchange, String routingKey, Object message)
      throws Throwable {
    final Object[] args = pjp.getArgs();

    Message convertedMessage = convertMessageIfNecessary(message);

    final MessageProperties messageProperties = convertedMessage.getMessageProperties();

    Scope scope = RabbitMqTracingUtils.buildSendSpan(tracer, messageProperties);
    tracer.inject(
        scope.span().context(),
        Format.Builtin.TEXT_MAP,
        new RabbitMqMessagePropertiesCarrier(messageProperties));

    RabbitMqSpanDecorator.StandardTags spanDecorator = new RabbitMqSpanDecorator.StandardTags();
    spanDecorator.onSend(messageProperties, exchange, routingKey, scope.span());

    args[2] = convertedMessage;

    try {
      return pjp.proceed(args);
    } catch (Exception ex) {
      spanDecorator.onError(ex, scope.span());
      throw ex;
    } finally {
      scope.close();
    }
  }
  // CHECKSTYLE:ON

  private Message convertMessageIfNecessary(final Object object) {
    if (object instanceof Message) {
      return (Message) object;
    }

    return messageConverter.toMessage(object, new MessageProperties());
  }
}

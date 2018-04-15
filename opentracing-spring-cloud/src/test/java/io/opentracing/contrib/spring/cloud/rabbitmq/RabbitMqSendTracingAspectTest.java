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

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;

import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockTracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gilles Robert
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MockTracingConfiguration.class
)
@RunWith(SpringJUnit4ClassRunner.class)
public class RabbitMqSendTracingAspectTest {

  private RabbitMqSendTracingAspect aspect;
  @Autowired private MockTracer mockTracer;
  @Mock private ProceedingJoinPoint proceedingJoinPoint;
  @Mock private MessageConverter messageConverter;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    TestUtils.verifyNoMoreInteractionsWithMocks(this);
  }

  @Test
  public void testTraceRabbitSend_whenNoPropertiesHeaders() throws Throwable {
    // given
    Span span = mockTracer.buildSpan("test").start();

    mockTracer.scopeManager().activate(span, false);

    aspect = new RabbitMqSendTracingAspect(mockTracer, messageConverter);

    String exchange = "opentracing.event.exchange";
    String routingKey = "io.opentracing.event.AnEvent";
    TestMessage<String> myMessage = new TestMessage<>("");

    Object[] args = new Object[] {exchange, routingKey, myMessage};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    MessageProperties properties = new MessageProperties();
    properties.setReceivedExchange("exchange");
    properties.setReceivedRoutingKey("routingKey");
    properties.setMessageId("messageId");
    Message message = new Message("".getBytes(), properties);
    given(messageConverter.toMessage(anyObject(), any(MessageProperties.class)))
        .willReturn(message);

    // when
    aspect.traceRabbitSend(proceedingJoinPoint, exchange, routingKey, myMessage);

    // then
    verify(proceedingJoinPoint).getArgs();
    verify(messageConverter).toMessage(anyObject(), any(MessageProperties.class));
    verify(proceedingJoinPoint).proceed(args);
  }

  @Test
  public void testTraceRabbitSend_whenNoConversionIsNeeded() throws Throwable {
    // given
    aspect = new RabbitMqSendTracingAspect(mockTracer, messageConverter);

    String exchange = "opentracing.event.exchange";
    String routingKey = "io.opentracing.event.AnEvent";

    MessageProperties properties = new MessageProperties();
    Message message = new Message("".getBytes(), properties);
    Object[] args = new Object[] {exchange, routingKey, message};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    given(messageConverter.toMessage(anyObject(), any(MessageProperties.class)))
        .willReturn(message);

    // when
    aspect.traceRabbitSend(proceedingJoinPoint, exchange, routingKey, message);

    // then
    verify(proceedingJoinPoint).getArgs();
    verify(proceedingJoinPoint).proceed(args);
  }

  @Test(expected = RuntimeException.class)
  public void testTraceRabbitSend_whenException() throws Throwable {
    // given
    aspect = new RabbitMqSendTracingAspect(mockTracer, messageConverter);

    String exchange = "opentracing.event.exchange";
    String routingKey = "io.opentracing.event.AnEvent";

    MessageProperties properties = new MessageProperties();
    Message message = new Message("".getBytes(), properties);
    Object[] args = new Object[] {exchange, routingKey, message};
    given(proceedingJoinPoint.getArgs()).willReturn(args);

    given(messageConverter.toMessage(anyObject(), any(MessageProperties.class)))
        .willReturn(message);

    given(proceedingJoinPoint.proceed(args)).willThrow(new RuntimeException());
    try {
      // when
      aspect.traceRabbitSend(proceedingJoinPoint, exchange, routingKey, message);
    } catch (RuntimeException e) {
      // then
      verify(proceedingJoinPoint).getArgs();
      verify(proceedingJoinPoint).proceed(args);

      throw e;
    }
  }

  class TestMessage<T> {

    private T body;

    TestMessage(T body) {
      this.body = body;
    }

    T getBody() {
      return body;
    }

    void setBody(T body) {
      this.body = body;
    }
  }
}

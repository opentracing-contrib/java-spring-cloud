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
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Note: these tests were adapted for Open Tracing from Spring Cloud Sleuth project.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = OpenTracingChannelInterceptorIT.App.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class OpenTracingChannelInterceptorIT implements MessageHandler {

  private static final String TRACE_ID_HEADER = "traceid";

  private static final String SPAN_ID_HEADER = "spanid";

  private static final String THROW_EXCEPTION_HEADER = "THROW_EXCEPTION";

  @Autowired
  @Qualifier("tracedChannel")
  private DirectChannel tracedChannel;

  @Autowired
  @Qualifier("pollableChannel")
  private PollableChannel pollableChannel;

  @Autowired
  private MockTracer mockTracer;

  @Autowired
  private MessagingTemplate messagingTemplate;

  private Message<?> message;

  @Override
  public void handleMessage(Message<?> message) throws MessagingException {
    this.message = message;
    if (message.getHeaders()
        .containsKey(THROW_EXCEPTION_HEADER)) {
      throw new RuntimeException("A terrible exception has occurred");
    }
  }

  @Before
  public void before() {
    tracedChannel.subscribe(this);
    mockTracer.reset();
  }

  @After
  public void after() {
    tracedChannel.unsubscribe(this);
    mockTracer.reset();
  }

  @Test
  public void shouldCreateSpan() {
    tracedChannel.send(MessageBuilder.withPayload("hi")
        .build());
    assertThat(message).isNotNull();
    then(message.getPayload()).isEqualTo("hi");
    then(message.getHeaders()).containsKeys(TRACE_ID_HEADER, SPAN_ID_HEADER);
    then(mockTracer.finishedSpans()).hasSize(1);
  }

  @Test
  public void shouldKeepHeadersMutable() {
    tracedChannel.send(MessageBuilder.withPayload("hi")
        .build());
    assertThat(message).isNotNull();

    MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
    assertThat(accessor).isNotNull();
  }

  @Test
  public void shouldPropagateTraceViaPollableChannel() {
    pollableChannel.send(MessageBuilder.withPayload("hi")
        .build());
    Message<?> message = pollableChannel.receive(0);
    assertThat(message).isNotNull();
    then(message.getPayload()).isEqualTo("hi");
    then(message.getHeaders()).containsKeys(TRACE_ID_HEADER, SPAN_ID_HEADER);
    then(mockTracer.finishedSpans()).hasSize(1);
  }

  @Test
  public void shouldCreateSpanWithExportedParent() {
    tracedChannel.send(MessageBuilder.withPayload("hi")
        .setHeader(TRACE_ID_HEADER, "100")
        .setHeader(SPAN_ID_HEADER, "200")
        .build());
    then(message).isNotNull();
    then(message.getHeaders()).containsKeys(TRACE_ID_HEADER, SPAN_ID_HEADER);
    then(mockTracer.finishedSpans()).hasSize(1);

    Long traceId = Long.valueOf(message.getHeaders()
        .get(TRACE_ID_HEADER, String.class));
    Long spanId = Long.valueOf(message.getHeaders()
        .get(SPAN_ID_HEADER, String.class));

    then(traceId).isEqualTo(100);
    then(spanId).isNotEqualTo(200);

    MockSpan finishedSpan = mockTracer.finishedSpans()
        .get(0);
    then(finishedSpan.parentId()).isEqualTo(200);

    MockSpan.MockContext finishedSpanContext = finishedSpan.context();
    then(finishedSpanContext.traceId()).isEqualTo(traceId);
    then(finishedSpanContext.spanId()).isEqualTo(spanId);
  }

  @Test
  public void shouldCreateSpanWithActiveParent() {
    MockSpan parentSpan = mockTracer.buildSpan("http:testSendMessage")
        .start();
    mockTracer.makeActive(parentSpan);

    tracedChannel.send(MessageBuilder.withPayload("hi")
        .build());
    then(message).isNotNull();
    then(message.getHeaders()).containsKeys(TRACE_ID_HEADER, SPAN_ID_HEADER);
    then(mockTracer.finishedSpans()).hasSize(1); // TODO because parent span is not finished

    Long traceId = Long.valueOf(message.getHeaders()
        .get(TRACE_ID_HEADER, String.class));
    Long spanId = Long.valueOf(message.getHeaders()
        .get(SPAN_ID_HEADER, String.class));

    MockSpan.MockContext parentSpanContext = parentSpan.context();

    then(traceId).isEqualTo(parentSpanContext.traceId());
    then(spanId).isNotEqualTo(parentSpanContext.spanId());

    MockSpan finishedSpan = mockTracer.finishedSpans()
        .get(0);
    then(finishedSpan.parentId()).isEqualTo(parentSpanContext.spanId());

    MockSpan.MockContext finishedSpanContext = finishedSpan.context();
    then(finishedSpanContext.traceId()).isEqualTo(traceId);
    then(finishedSpanContext.spanId()).isEqualTo(spanId);
  }

  @Test
  public void shouldCreateHeadersWhenUsingMessagingTemplate() {
    messagingTemplate.send(MessageBuilder.withPayload("hi")
        .build());
    then(message).isNotNull();
    then(message.getHeaders()).containsKeys(TRACE_ID_HEADER, SPAN_ID_HEADER);
    then(mockTracer.finishedSpans()).hasSize(1);
  }

  @Test
  public void shouldCloseSpanWhenExceptionOccurred() {
    try {
      messagingTemplate.send(MessageBuilder.withPayload("hi")
          .setHeader(THROW_EXCEPTION_HEADER, true)
          .build());
      fail("Exception should occur");
    } catch (RuntimeException ignored) {
      // Expected exception
    }

    then(message).isNotNull();
    then(mockTracer.finishedSpans()).hasSize(1);
    MockSpan span = mockTracer.finishedSpans()
        .get(0);
    then(span.logEntries()).hasSize(3);
    then(span.logEntries()
        .get(0)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(0)
        .fields()
        .get("event")).isEqualTo(Events.CLIENT_SEND);
    then(span.logEntries()
        .get(1)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(1)
        .fields()
        .get("event")).isEqualTo(Events.CLIENT_RECEIVE);
    then(span.logEntries()
        .get(2)
        .fields()).containsOnlyKeys("error");
    then(span.logEntries()
        .get(2)
        .fields()
        .get("error")).isEqualTo("A terrible exception has occurred");
  }

  @Test
  public void shouldLogClientReceivedClientSentEventWhenTheMessageIsSentAndReceived() {
    tracedChannel.send(MessageBuilder.withPayload("hi")
        .build());

    then(mockTracer.finishedSpans()).hasSize(1);
    MockSpan span = mockTracer.finishedSpans()
        .get(0);
    then(span.logEntries()).hasSize(2);

    then(span.logEntries()
        .get(0)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(0)
        .fields()
        .get("event")).isEqualTo(Events.CLIENT_SEND);
    then(span.logEntries()
        .get(1)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(1)
        .fields()
        .get("event")).isEqualTo(Events.CLIENT_RECEIVE);
  }

  @Test
  public void shouldLogServerReceivedServerSentEventWhenTheMessageIsPropagatedToTheNextListener() {
    tracedChannel.send(MessageBuilder.withPayload("hi")
        .setHeader(Headers.MESSAGE_SENT_FROM_CLIENT, true)
        .build());

    then(mockTracer.finishedSpans()).hasSize(1);
    MockSpan span = mockTracer.finishedSpans()
        .get(0);
    then(span.logEntries()).hasSize(2);

    then(span.logEntries()
        .get(0)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(0)
        .fields()
        .get("event")).isEqualTo(Events.SERVER_RECEIVE);
    then(span.logEntries()
        .get(1)
        .fields()).containsOnlyKeys("event");
    then(span.logEntries()
        .get(1)
        .fields()
        .get("event")).isEqualTo(Events.SERVER_SEND);
  }

  @Test
  @Ignore // TODO patterns are not yet supported
  public void shouldNotTraceIgnoredChannel() {

  }

  @Test
  @Ignore // TODO channel name limit is not yet supported
  public void downgrades128bitIdsByDroppingHighBits() {

  }

  @Test
  @Ignore // TODO header sanitation is not yet supported
  public void shouldNotBreakWhenInvalidHeadersAreSent() {

  }

  @Configuration
  @EnableAutoConfiguration
  static class App {

    @Bean
    public ThreadLocalActiveSpanSource threadLocalActiveSpanSource() {
      return new ThreadLocalActiveSpanSource();
    }

    @Bean
    public MockTracer mockTracer(ThreadLocalActiveSpanSource threadLocalActiveSpanSource) {
      return new MockTracer(threadLocalActiveSpanSource, MockTracer.Propagator.TEXT_MAP);
    }

    @Bean
    public DirectChannel tracedChannel() {
      return new DirectChannel();
    }

    @Bean
    public PollableChannel pollableChannel() {
      return new QueueChannel();
    }

    @Bean
    public MessagingTemplate messagingTemplate() {
      return new MessagingTemplate(tracedChannel());
    }

    @Bean
    @GlobalChannelInterceptor
    public OpenTracingChannelInterceptor openTracingChannelInterceptor(Tracer tracer) {
      return new OpenTracingChannelInterceptor(tracer);
    }

  }

}

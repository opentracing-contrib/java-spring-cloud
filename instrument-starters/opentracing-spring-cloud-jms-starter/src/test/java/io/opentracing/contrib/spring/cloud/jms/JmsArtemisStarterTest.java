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
package io.opentracing.contrib.spring.cloud.jms;

import static org.awaitility.Awaitility.await;
import static org.jgroups.util.Util.assertEquals;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        MockTracingConfiguration.class,
        JmsArtemisStarterTest.JmsTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsArtemisStarterTest {
  private static final String QUEUE_NAME = "testQueue";

  @Configuration
  @EnableJms
  static class JmsTestConfiguration {

    @Bean
    public MsgListener msgListener() {
      return new MsgListener();
    }

    @Bean
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory,
        DefaultJmsListenerContainerFactoryConfigurer configurer) {
      DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
      // This provides all boot's default to this factory, including the message converter
      configurer.configure(factory, connectionFactory);
      // You could still override some of Boot's default if necessary.
      return factory;
    }

    public static class MsgListener {

      private Message message;

      public Message getMessage() {
        return message;
      }

      @JmsListener(destination = QUEUE_NAME, containerFactory = "myFactory")
      public void processMessage(Message msg) throws Exception {
        message = msg;
      }
    }
  }

  @Autowired
  private MockTracer tracer;

  @Autowired
  private JmsTestConfiguration.MsgListener msgListener;

  @Autowired
  private JmsTemplate jmsTemplate;

  @Before
  public void before() {
    tracer.reset();
  }

  @Test
  public void testListenerSpans() throws JMSException {
    jmsTemplate.convertAndSend(QUEUE_NAME, "huhu");

    await().until(() -> {
      List<MockSpan> mockSpans = tracer.finishedSpans();
      return (mockSpans.size() == 2);
    });

    Message message = msgListener.getMessage();
    assertEquals("huhu", ((TextMessage)msgListener.getMessage()).getText());
    List<MockSpan> spans = tracer.finishedSpans();
    // HTTP server span, jms send, jms receive
    assertEquals(2, spans.size());
    TestUtils.assertSameTraceId(spans);
  }
}

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
import static org.junit.Assert.assertEquals;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SpringBootTest(
    classes = {MockTracingConfiguration.class,
        JmsArtemisManualServerTest.JmsTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsArtemisManualServerTest {
  private static final String QUEUE_NAME = "testQueue";

  @org.springframework.context.annotation.Configuration
  static class JmsTestConfiguration {

    private static final Logger log = Logger.getLogger(JmsTestConfiguration.class.getName());

    @Bean
    public MsgListener msgListener() {
      return new MsgListener();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server server() {
      return new Server();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
      return new ActiveMQJMSConnectionFactory("vm://0");
    }

    public static class Server {

      private ActiveMQServer server;

      public void start() throws Exception {
        org.apache.activemq.artemis.core.config.Configuration configuration = new ConfigurationImpl();

        HashSet<TransportConfiguration> transports = new HashSet<>();
        transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
        configuration.setAcceptorConfigurations(transports);
        configuration.setSecurityEnabled(false);

        File targetDir = new File(System.getProperty("user.dir") + "/target");
        configuration.setBrokerInstance(targetDir);

        ActiveMQServer temp = new ActiveMQServerImpl(configuration);
        temp.start();

        server = temp;
      }

      public void stop() throws Exception {
        if (server != null) {
          server.stop();
        }
      }
    }

    public static class MsgListener {

      private Message message;

      public Message getMessage() {
        return message;
      }

      @JmsListener(destination = QUEUE_NAME)
      public void processMessage(Message msg) throws Exception {
        log.info("Received msg: " + msg.toString());
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
  public void before() throws Exception {
    tracer.reset();
  }

  @Test
  public void testListenerSpans() throws JMSException {
    jmsTemplate.convertAndSend(QUEUE_NAME, "Hello!");

    await().atMost(10, TimeUnit.SECONDS).until(() -> {
      List<MockSpan> mockSpans = tracer.finishedSpans();
      return (mockSpans.size() == 2);
    });

    Assert.assertEquals("Hello!", ((TextMessage)msgListener.getMessage()).getText());

    List<MockSpan> spans = tracer.finishedSpans();
    // jms send, jms receive
    assertEquals(2, spans.size());
    TestUtils.assertSameTraceId(spans);
  }
}

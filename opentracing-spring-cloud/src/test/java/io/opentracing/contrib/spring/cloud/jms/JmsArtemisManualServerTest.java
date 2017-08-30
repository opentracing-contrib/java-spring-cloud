package io.opentracing.contrib.spring.cloud.jms;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {MockTracingConfiguration.class,
          JmsArtemisManualServerTest.JmsTestConfiguration.class,
          MsgController.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsArtemisManualServerTest {

  @Configuration
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
        transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
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

      @JmsListener(destination = "fooQueue")
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
  private TestRestTemplate restTemplate;

  @Before
  public void before() {
    tracer.reset();
  }

  @Test
  public void testListenerSpans() {
    ResponseEntity<String> responseEntity = restTemplate.getForEntity("/hello", String.class);

    await().until(() -> {
      List<MockSpan> mockSpans = tracer.finishedSpans();
      return (mockSpans.size() == 3);
    });

    Assert.assertNotNull(msgListener.getMessage());

    assertEquals(200, responseEntity.getStatusCode().value());
    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(3, spans.size());  // it propagated over to @JmsListener
    TestUtils.assertSameTraceId(spans);
  }
}

package io.opentracing.contrib.spring.cloud.jms;

import java.io.File;
import java.util.HashSet;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Configuration
public class JmsTestConfiguration {
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

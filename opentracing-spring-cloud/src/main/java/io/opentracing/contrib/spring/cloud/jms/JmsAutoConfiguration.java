package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.Message;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Configuration
@ConditionalOnClass(Message.class)
public class JmsAutoConfiguration {

  @Bean
  @ConditionalOnClass(JmsListener.class)
  public JmsListenerAspect createJmsListenerAspect() {
    return new JmsListenerAspect();
  }

}
package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import io.opentracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaderMapper;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Configuration
@ConditionalOnClass(Message.class)
public class JmsAutoConfiguration {

  @Bean
  @ConditionalOnClass(JmsListener.class)
  public JmsListenerAspect jmsListenerAspect() {
    return new JmsListenerAspect();
  }

  @Bean
  @ConditionalOnClass(JmsTemplate.class)
  @ConditionalOnBean(ConnectionFactory.class)
  public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, Tracer tracer, JmsHeaderMapper mapper) {
    return new TracingJmsTemplate(connectionFactory, tracer, mapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public JmsHeaderMapper fromTracer(Tracer tracer) {
    return TracingJmsHeaderMapper.fromTracer(tracer);
  }
}
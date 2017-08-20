package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms.spring.TracingJmsTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnClass({Message.class, JmsTemplate.class})
@ConditionalOnBean(Tracer.class)
public class JmsAutoConfiguration {

  @Bean
  public JmsListenerAspect jmsListenerAspect() {
    return new JmsListenerAspect();
  }

  @Bean
  public JmsTemplate jmsTemplate(BeanFactory beanFactory, Tracer tracer) {
    // we create lazy proxy, to avoid dependency and config order
    // if JMS is used, and ConnectionFactory bean is not present,
    // it will throw an error on first use, so imo, we should be all good
    ConnectionFactory connectionFactory = createProxy(beanFactory);
    return new TracingJmsTemplate(connectionFactory, tracer);
  }

  private ConnectionFactory createProxy(final BeanFactory beanFactory) {
    return (ConnectionFactory) ProxyFactory.getProxy(new AbstractLazyCreationTargetSource() {
      @Override
      public synchronized Class<?> getTargetClass() {
        return ConnectionFactory.class;
      }

      @Override
      protected Object createObject() throws Exception {
        return beanFactory.getBean(ConnectionFactory.class);
      }
    });
  }
}

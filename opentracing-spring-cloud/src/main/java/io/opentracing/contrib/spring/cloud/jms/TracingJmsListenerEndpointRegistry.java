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
package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jms.common.TracingMessageListener;
import io.opentracing.contrib.jms.common.TracingMessageUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TracingJmsListenerEndpointRegistry extends JmsListenerEndpointRegistry implements BeanFactoryAware {
  private final Tracer tracer;

  private BeanFactory beanFactory;
  private JmsListenerEndpointRegistrar registrar;

  public TracingJmsListenerEndpointRegistry(Tracer tracer) {
    Assert.notNull(tracer, "Null tracer");
    this.tracer = tracer;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  void setRegistrar(JmsListenerEndpointRegistrar registrar) {
    this.registrar = registrar;
  }

  @Override
  public void registerListenerContainer(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> factory, boolean startImmediately) {
    if (endpoint instanceof MethodJmsListenerEndpoint) {
      endpoint = replaceMethodJmsListenerEndpoint((MethodJmsListenerEndpoint) endpoint);
    }
    super.registerListenerContainer(endpoint, factory, startImmediately);
  }

  private JmsListenerEndpoint replaceMethodJmsListenerEndpoint(MethodJmsListenerEndpoint original) {
    MethodJmsListenerEndpoint replacement = new TracingMethodJmsListenerEndpoint();

    replacement.setBean(original.getBean());
    replacement.setMethod(original.getMethod());
    replacement.setMostSpecificMethod(original.getMostSpecificMethod());
    MessageHandlerMethodFactory messageHandlerMethodFactory = registrar.getMessageHandlerMethodFactory();
    if (messageHandlerMethodFactory == null) {
      messageHandlerMethodFactory = createDefaultJmsHandlerMethodFactory();
    }
    replacement.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
    replacement.setBeanFactory(beanFactory);
    replacement.setId(original.getId());
    replacement.setDestination(original.getDestination());
    replacement.setSelector(original.getSelector());
    replacement.setSubscription(original.getSubscription());
    replacement.setConcurrency(original.getConcurrency());

    return replacement;
  }

  private class TracingMethodJmsListenerEndpoint extends MethodJmsListenerEndpoint {
    @Override
    protected MessagingMessageListenerAdapter createMessageListenerInstance() {
      return new TracingMessagingMessageListenerAdapter();
    }
  }

  private class TracingMessagingMessageListenerAdapter extends MessagingMessageListenerAdapter {
    @Override
    public void onMessage(final Message jmsMessage, final Session session) throws JMSException {
      TracingMessageListener listener = new TracingMessageListener(message -> onMessageInternal(message, session), tracer);
      listener.onMessage(jmsMessage);
    }

    private void onMessageInternal(Message jmsMessage, Session session) {
      try {
        super.onMessage(jmsMessage, session);
      } catch (JMSException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    protected void sendResponse(Session session, Destination destination, Message response) throws JMSException {
      Span span = TracingMessageUtils.buildAndInjectSpan(destination, response, tracer);
      try {
        super.sendResponse(session, destination, response);
      } finally {
        span.finish();
      }
    }
  }

  private MessageHandlerMethodFactory createDefaultJmsHandlerMethodFactory() {
    DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
    defaultFactory.setBeanFactory(beanFactory);
    defaultFactory.afterPropertiesSet();
    return defaultFactory;
  }

}

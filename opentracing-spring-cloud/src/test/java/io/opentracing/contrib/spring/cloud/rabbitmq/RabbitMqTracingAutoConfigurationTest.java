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

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import io.opentracing.Tracer;
import java.lang.reflect.Field;
import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;


/**
 * @author Gilles Robert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {RabbitMqTracingAutoConfigurationTest.TestConfig.class, RabbitMqTracingAutoConfiguration.class})
public class RabbitMqTracingAutoConfigurationTest {

  @MockBean
  private Tracer tracer;
  @Autowired
  private RabbitMqSendTracingAspect tracingAspect;
  @Autowired
  private RabbitMqReceiveTracingInterceptor tracingInterceptor;
  @Autowired
  private SimpleMessageListenerContainer simpleMessageListenerContainer;
  @Autowired
  private SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;

  @Test
  public void testRabbitMqTracingEnabled() {
    assertNotNull(tracingAspect);
    assertNotNull(tracingInterceptor);

    assertNotNull(simpleMessageListenerContainer);
    Field adviceChainField =
              ReflectionUtils.findField(SimpleMessageListenerContainer.class, "adviceChain");
    ReflectionUtils.makeAccessible(adviceChainField);
    Advice[] chain = (Advice[]) ReflectionUtils.getField(adviceChainField, simpleMessageListenerContainer);
    assertThat(chain.length, is(1));
    assertThat(chain[0], instanceOf(RabbitMqReceiveTracingInterceptor.class));

    assertNotNull(simpleRabbitListenerContainerFactory);
    Advice[] adviceChain = simpleRabbitListenerContainerFactory.getAdviceChain();
    assertThat(adviceChain.length, is(1));
    assertThat(adviceChain[0], instanceOf(RabbitMqReceiveTracingInterceptor.class));
  }

  @Configuration
  static class TestConfig {

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private MessageConverter messageConverter = mock(MessageConverter.class);

    @Bean
    public RabbitTemplate rabbitTemplate() {
      RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
      rabbitTemplate.setMessageConverter(messageConverter);
      return rabbitTemplate;
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer() {
      SimpleMessageListenerContainer container =
              new SimpleMessageListenerContainer(connectionFactory);
      container.setMessageConverter(messageConverter);
      return container;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
      SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
      factory.setConnectionFactory(connectionFactory);
      factory.setMessageConverter(messageConverter);
      return factory;
    }
  }
}

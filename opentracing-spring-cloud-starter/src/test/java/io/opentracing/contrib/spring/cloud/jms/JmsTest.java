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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms.spring.TracingJmsTemplate;
import javax.jms.ConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {JmsTest.JmsEmbeddedArtemisConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsTest {

  @Configuration
  @EnableJms
  @EnableAutoConfiguration
  static class JmsEmbeddedArtemisConfiguration {

    @Bean
    public Tracer tracer() {
      return mock(Tracer.class);
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
  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void testTracingFeignBeanCreated() {
    assertNotNull(applicationContext.getBean(TracingJmsTemplate.class));
  }
}

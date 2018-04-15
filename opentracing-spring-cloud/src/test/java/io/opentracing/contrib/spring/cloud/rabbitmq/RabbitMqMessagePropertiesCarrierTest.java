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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import org.springframework.amqp.core.MessageProperties;


/**
 * @author Gilles Robert
 */
public class RabbitMqMessagePropertiesCarrierTest {

  @Test
  public void testPut() {
    // given
    MessageProperties messageProperties = new MessageProperties();
    RabbitMqMessagePropertiesCarrier carrier =
        new RabbitMqMessagePropertiesCarrier(messageProperties);
    String key = "myKey";
    String value = "myValue";

    // when
    carrier.put(key, value);

    // then
    assertThat(messageProperties.getHeaders().get(key)).isEqualTo(value);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testIterator() {
    // given
    MessageProperties messageProperties = new MessageProperties();
    RabbitMqMessagePropertiesCarrier carrier =
        new RabbitMqMessagePropertiesCarrier(messageProperties);

    // when
    carrier.iterator();

    // then exception
  }
}

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

package io.opentracing.contrib.spring.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageTextMapTest {

  @Test
  public void shouldGetIterator() {
    Map<String, String> headers = new HashMap<>(2);
    headers.put("h1", "v1");
    headers.put("h2", "v2");
    Message<String> message = MessageBuilder.withPayload("test")
        .copyHeaders(headers)
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);

    assertThat(map.iterator()).containsAll(headers.entrySet());
  }

  @Test
  public void shouldPutEntry() {
    Message<String> message = MessageBuilder.withPayload("test")
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);
    map.put("k1", "v1");

    assertThat(map.iterator()).contains(new AbstractMap.SimpleEntry<>("k1", "v1"));
  }

  @Test
  public void shouldGetMessageWithNewHeaders() {
    Message<String> message = MessageBuilder.withPayload("test")
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);
    map.put("k1", "v1");
    Message<String> updatedMessage = map.getMessage();

    assertThat(updatedMessage.getPayload()).isEqualTo(message.getPayload());
    assertThat(updatedMessage.getHeaders()).contains(new AbstractMap.SimpleEntry<>("k1", "v1"));
  }

}

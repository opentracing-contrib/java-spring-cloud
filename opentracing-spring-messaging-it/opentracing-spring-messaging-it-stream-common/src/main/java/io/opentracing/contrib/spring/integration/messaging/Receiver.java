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

import java.util.ArrayList;
import java.util.List;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@EnableBinding(Sink.class)
public class Receiver {

  private final List<Message> receivedMessages = new ArrayList<>();

  @StreamListener(Sink.INPUT)
  public void receive(Message message) {
    receivedMessages.add(message);
  }

  public List<Message> getReceivedMessages() {
    return receivedMessages;
  }

  public void clear() {
    receivedMessages.clear();
  }

}

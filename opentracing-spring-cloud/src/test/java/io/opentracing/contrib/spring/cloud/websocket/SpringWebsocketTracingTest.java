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
package io.opentracing.contrib.spring.cloud.websocket;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockTracer;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {WebSocketConfig.class, GreetingController.class,
    MockTracingConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringWebsocketTracingTest {

  private static final String SEND_HELLO_MESSAGE_ENDPOINT = "/app/hello";
  private static final String SUBSCRIBE_GREETINGS_ENDPOINT = "/topic/greetings";

  @Value("${local.server.port}")
  private int port;
  private String url;

  @Autowired
  private MockTracer mockTracer;

  @Before
  public void setup() {
    url = "ws://localhost:" + port + "/test-websocket";
  }

  @Test
  public void testTracedWebsocketSession()
      throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
    WebSocketStompClient stompClient = new WebSocketStompClient(
        new SockJsClient(createTransportClient()));
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());

    StompSession stompSession = stompClient.connect(url, new StompSessionHandlerAdapter() {
    }).get(1, TimeUnit.SECONDS);

    stompSession.subscribe(SUBSCRIBE_GREETINGS_ENDPOINT, new GreetingStompFrameHandler());
    stompSession.send(SEND_HELLO_MESSAGE_ENDPOINT, new HelloMessage("Hi"));

    // Three spans related to the websocket session, and two related to websocket connect
    await().until(() -> mockTracer.finishedSpans().size() == 5);

    assertTrue(mockTracer.finishedSpans().stream().filter(s ->
        s.operationName().equals(SEND_HELLO_MESSAGE_ENDPOINT)).toArray().length == 1);
    assertTrue(mockTracer.finishedSpans().stream().filter(s ->
        s.operationName().equals(SUBSCRIBE_GREETINGS_ENDPOINT)).toArray().length == 1);
    assertTrue(mockTracer.finishedSpans().stream().filter(s ->
        s.operationName().equals(GreetingController.DOING_WORK)).toArray().length == 1);
  }

  private List<Transport> createTransportClient() {
    return Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
  }

  private class GreetingStompFrameHandler implements StompFrameHandler {

    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
      return Greeting.class;
    }

    @Override
    public void handleFrame(StompHeaders stompHeaders, Object o) {
    }
  }
}

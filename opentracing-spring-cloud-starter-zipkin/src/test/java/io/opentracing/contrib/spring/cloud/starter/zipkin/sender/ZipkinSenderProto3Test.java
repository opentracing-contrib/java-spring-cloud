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
package io.opentracing.contrib.spring.cloud.starter.zipkin.sender;

import static org.junit.Assert.assertEquals;

import io.opentracing.contrib.spring.cloud.starter.zipkin.AbstractZipkinTracerSenderSpringTest;
import java.io.IOException;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Pavol Loffay
 */
@TestPropertySource(
    properties = {
        "opentracing.zipkin.http-sender.encoder=PROTO3"
    }
)
public class ZipkinSenderProto3Test extends AbstractZipkinTracerSenderSpringTest {
  @Test
  public void testUrl() {
    assertSenderUrl("http://localhost:9411/api/v2/spans");
  }

  @Test
  public void testEncoding() throws InterruptedException, IOException {
    MockWebServer server = new MockWebServer();
    server.start(9411);

    tracer.buildSpan("foo").start().finish();
    RecordedRequest recordedRequest = server.takeRequest();
    assertEquals("application/x-protobuf", recordedRequest.getHeader("Content-Type"));
    server.shutdown();
  }
}

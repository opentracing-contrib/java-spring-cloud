/*
 * Copyright © 2017-2026 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentracing.contrib.spring.cloud.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

public class Log4j2SpanLogsAppenderTest {

  @Test
  public void appendsLogEventToActiveSpan() {
    MockTracer tracer = new MockTracer();
    MockSpan span = tracer.buildSpan("request").start();
    Log4j2SpanLogsAppender appender = new Log4j2SpanLogsAppender(tracer);
    appender.start();

    try (io.opentracing.Scope ignored = tracer.scopeManager().activate(span)) {
      appender.append(Log4jLogEvent.newBuilder()
          .setLoggerName("example.Controller")
          .setLevel(Level.ERROR)
          .setThreadName("request-thread")
          .setMessage(new SimpleMessage("request failed"))
          .setTimeMillis(1000L)
          .setThrown(new IllegalStateException("failure"))
          .build());
    }

    assertEquals(1, span.logEntries().size());
    MockSpan.LogEntry entry = span.logEntries().get(0);
    assertEquals("example.Controller", entry.fields().get("logger"));
    assertEquals("ERROR", entry.fields().get("level"));
    assertEquals("request-thread", entry.fields().get("thread"));
    assertEquals("request failed", entry.fields().get("message"));
    assertEquals(1000000L, entry.timestampMicros());
    assertSame(IllegalStateException.class, ((Throwable) entry.fields().get("error.object")).getClass());
    assertEquals(Tags.ERROR.getKey(), entry.fields().get("event"));
    assertEquals(Boolean.TRUE, span.tags().get(Tags.ERROR.getKey()));
    span.finish();
  }
}

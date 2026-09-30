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

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class Log4j2SpanLogsAppender extends AbstractAppender {

  private final Tracer tracer;

  public Log4j2SpanLogsAppender(Tracer tracer) {
    super(Log4j2SpanLogsAppender.class.getSimpleName(), null, null, true, Property.EMPTY_ARRAY);
    this.tracer = tracer;
  }

  @Override
  public void append(LogEvent event) {
    Span span = tracer.activeSpan();
    if (span == null) {
      return;
    }

    Map<String, Object> logs = new HashMap<>(6);
    logs.put("logger", event.getLoggerName());
    logs.put("level", event.getLevel().toString());
    logs.put("thread", event.getThreadName());
    logs.put("message", event.getMessage().getFormattedMessage());

    if (Level.ERROR.equals(event.getLevel())) {
      Tags.ERROR.set(span, Boolean.TRUE);
      logs.put("event", Tags.ERROR.getKey());
    }

    if (event.getThrown() != null) {
      logs.put("error.object", event.getThrown());
    }
    span.log(TimeUnit.MICROSECONDS.convert(event.getTimeMillis(), TimeUnit.MILLISECONDS), logs);
  }
}

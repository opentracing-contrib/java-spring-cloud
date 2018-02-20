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
package io.opentracing.contrib.spring.cloud.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavol Loffay
 */
public class SpanLogsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final Tracer tracer;

  public SpanLogsAppender(Tracer tracer) {
    this.name = SpanLogsAppender.class.getSimpleName();
    this.tracer = tracer;
  }

  /**
   * This is called only for configured levels.
   * It will not be executed for DEBUG level if root logger is INFO.
   */
  @Override
  protected void append(ILoggingEvent event) {
    Span span = tracer.activeSpan();
    if (span != null) {
      Map<String, Object> logs = new HashMap<>(6);
      logs.put("logger", event.getLoggerName());
      logs.put("level", event.getLevel().toString());
      logs.put("thread", event.getThreadName());
      logs.put("message", event.getFormattedMessage());

      if (Level.ERROR.equals(event.getLevel())) {
        logs.put("event", Tags.ERROR);
      }

      IThrowableProxy throwableProxy = event.getThrowableProxy();
      if (throwableProxy instanceof ThrowableProxy) {
        Throwable throwable = ((ThrowableProxy)throwableProxy).getThrowable();
        // String stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        if (throwable != null) {
          logs.put("error.object", throwable);
        }
      }
      span.log(TimeUnit.MICROSECONDS.convert(event.getTimeStamp(), TimeUnit.MILLISECONDS), logs);
    }
  }
}

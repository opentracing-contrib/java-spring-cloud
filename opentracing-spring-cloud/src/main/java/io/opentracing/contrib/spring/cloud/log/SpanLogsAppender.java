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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavol Loffay
 */
public class SpanLogsAppender implements Appender<ILoggingEvent> {

  private final String name = SpanLogsAppender.class.getSimpleName();
  private final Tracer tracer;

  public SpanLogsAppender(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public String getName() {
    return this.name;
  }

  /**
   * This is called only for configured levels.
   * It will not be executed for DEBUG level if root logger is INFO.
   */
  @Override
  public void doAppend(ILoggingEvent event) throws LogbackException {
    Span span = tracer.activeSpan();
    if (span != null) {
      Map<String, String> logs = new HashMap<>(4);
      logs.put("logger", event.getLoggerName());
      logs.put("level", event.getLevel().toString());
      logs.put("thread", event.getThreadName());
      logs.put("message", event.getFormattedMessage());
      span.log(TimeUnit.MICROSECONDS.convert(event.getTimeStamp(), TimeUnit.MILLISECONDS), logs);
    }
  }

  @Override
  public void setName(String name) {
  }

  @Override
  public void setContext(Context context) {
  }

  @Override
  public Context getContext() {
    return null;
  }

  @Override
  public void addStatus(Status status) {
  }

  @Override
  public void addInfo(String msg) {
  }

  @Override
  public void addInfo(String msg, Throwable ex) {
  }

  @Override
  public void addWarn(String msg) {
  }

  @Override
  public void addWarn(String msg, Throwable ex) {
  }

  @Override
  public void addError(String msg) {
  }

  @Override
  public void addError(String msg, Throwable ex) {
  }

  @Override
  public void addFilter(Filter<ILoggingEvent> newFilter) {
  }

  @Override
  public void clearAllFilters() {
  }

  @Override
  public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
    return null;
  }

  @Override
  public FilterReply getFilterChainDecision(ILoggingEvent event) {
    return FilterReply.NEUTRAL;
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public boolean isStarted() {
    return true;
  }
}
